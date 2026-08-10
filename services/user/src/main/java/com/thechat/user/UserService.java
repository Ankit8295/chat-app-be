package com.thechat.user;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.common.dto.PageResponse;
import com.thechat.friendship.Friendship;
import com.thechat.friendship.FriendshipRepository;
import com.thechat.friendship.FriendshipStatus;
import com.thechat.friendship.dto.FriendResponse;
import com.thechat.object_storage.CloudflareR2Client;
import com.thechat.user.dto.AvatarConfirmRequest;
import com.thechat.user.dto.AvatarPresignRequest;
import com.thechat.user.dto.ProfilePresignedUrlResponse;
import com.thechat.user.dto.UpdateUserProfileRequest;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final FriendshipRepository friendshipRepository;
    private final ProfileImageRepository profileImageRepository;
    private final CloudflareR2Client r2Client;

    public UserService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            UserPreferenceRepository userPreferenceRepository,
            ProfileImageRepository profileImageRepository,
            CloudflareR2Client r2Client) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.profileImageRepository = profileImageRepository;
        this.r2Client = r2Client;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSearchResultResponse> searchUsers(UUID requesterId, String search, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<AppUser> matchedUsersPage = userRepository.searchUsersExcludingSelf(requesterId, search, pageable);

        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(requesterId, FriendshipStatus.ACTIVE);

        Map<UUID, String> friendshipStatusMap = friendships.stream()
                .collect(Collectors.toMap(
                        f -> f.getFriendUser().getId(),
                        f -> f.getStatus().name().toLowerCase()));

        List<UserSearchResultResponse> content = matchedUsersPage.getContent().stream()
                .map(user -> {
                    String status = friendshipStatusMap.getOrDefault(user.getId(), "none");
                    return UserSearchResultResponse.of(user, toPublicImageUrl(user.getImage()), status);
                })
                .toList();

        return PageResponse.of(content, page, size, matchedUsersPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<FriendResponse> getFriends(UUID requesterId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Friendship> friendsPage = friendshipRepository.findAllFriendsWithUserByUserIdAndStatus(requesterId,
                FriendshipStatus.ACTIVE, pageable);

        List<FriendResponse> content = friendsPage.getContent().stream()
                .map(friendship -> FriendResponse.from(
                        friendship,
                        toPublicImageUrl(friendship.getFriendUser().getImage())))
                .toList();

        return PageResponse.of(content, page, size, friendsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public UserPreferenceResponse getUserPreference(UUID id) {
        return userPreferenceRepository.findById(id)
                .map(UserPreferenceResponse::from)
                .orElseGet(() -> new UserPreferenceResponse(id, null));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getProfilesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(ids).stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public void ensureFriendship(UUID userId, UUID friendUserId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        AppUser friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new UserNotFoundException(friendUserId));

        if (!friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)) {
            friendshipRepository.save(new Friendship(user, friend, FriendshipStatus.ACTIVE));
        }
        if (!friendshipRepository.existsByUserIdAndFriendUserId(friendUserId, userId)) {
            friendshipRepository.save(new Friendship(friend, user, FriendshipStatus.ACTIVE));
        }
    }

    @Transactional
    public void createProfile(UUID userId, String email, String name) {
        if (userRepository.existsById(userId)) {
            return;
        }
        AppUser user = new AppUser(userId, email, name);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.name() != null && !request.name().isBlank()
                && !request.name().equals(user.getName())) {
            user.setName(request.name().trim());
        }

        if (request.about() != null && !request.about().isBlank()) {
            user.setAbout(request.about().trim());
        }

        // Avatar is set only via POST /me/avatar/confirm (object key), not PUT /me.

        return toUserResponse(user);
    }

    @Transactional
    public void deleteProfile(UUID userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public UserPreferenceResponse setUserPreference(UUID userId, UUID lastConversationId) {
        UserPreference preference = userPreferenceRepository.findById(userId)
                .map(existing -> {
                    existing.setLastConversationId(lastConversationId);
                    return existing;
                })
                .orElseGet(() -> {
                    AppUser user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFoundException(userId));
                    UserPreference newPreference = new UserPreference(user, lastConversationId);
                    return userPreferenceRepository.save(newPreference);
                });

        return UserPreferenceResponse.from(preference);
    }

    @Transactional
    public ProfilePresignedUrlResponse createAvatarPresign(UUID userId, AvatarPresignRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String contentType = normalizeContentType(request.contentType());
        long sizeBytes = requireValidSize(request.sizeBytes());
        String originalFileName = sanitizeFileName(request.fileName(), contentType);
        String extension = extensionForContentType(contentType);
        // Stable key so re-uploads overwrite the previous avatar object.
        String objectKey = "profiles/" + userId + "/" + userId + "." + extension;

        ProfileImage profileImage = profileImageRepository.save(
                new ProfileImage(user, objectKey, originalFileName, contentType, sizeBytes));

        String uploadUrl = r2Client.createPresignedPutUrl(objectKey, contentType);

        return new ProfilePresignedUrlResponse(
                profileImage.getId(),
                objectKey,
                uploadUrl,
                "PUT",
                r2Client.putUrlTtlSeconds(),
                Map.of("Content-Type", contentType));
    }

    @Transactional
    public UserResponse confirmAvatarUpload(UUID userId, AvatarConfirmRequest request) {
        ProfileImage profileImage = profileImageRepository
                .findByIdAndUser_Id(request.mediaId(), userId)
                .orElseThrow(() -> new ProfileImageNotFoundException(request.mediaId()));

        if (profileImage.getStatus() != ProfileImageStatus.PENDING) {
            throw new IllegalArgumentException("Profile image is not pending confirmation");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String previousKey = user.getImage();
        String newKey = profileImage.getObjectKey();

        profileImage.markUploaded();
        user.setImage(newKey);

        deleteReplacedAvatarObjects(userId, previousKey, newKey);

        return toUserResponse(user);
    }

    /**
     * If the previous avatar key differs (usually a different extension), delete
     * that R2
     * object using the key already stored on the user row — no bucket HEAD/GET
     * needed.
     * Best-effort: confirm still succeeds if delete fails.
     */
    private void deleteReplacedAvatarObjects(UUID userId, String previousKey, String newKey) {
        if (previousKey == null || previousKey.isBlank() || previousKey.equals(newKey)) {
            return;
        }

        String prefix = "profiles/" + userId + "/";
        if (!previousKey.startsWith(prefix)) {
            return;
        }

        deleteObjectBestEffort(previousKey);
    }

    private void deleteObjectBestEffort(String objectKey) {
        try {
            r2Client.deleteObject(objectKey);
        } catch (Exception exception) {
            log.warn("Failed to delete replaced avatar object key={}", objectKey, exception);
        }
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                toPublicImageUrl(user.getImage()),
                user.getAbout());
    }

    private String toPublicImageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String trimmed = objectKey.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return r2Client.publicUrl(trimmed);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_CONTENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported avatar contentType: " + contentType);
        }
        return normalized;
    }

    private static long requireValidSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes < 1) {
            throw new IllegalArgumentException("sizeBytes must be at least 1");
        }
        if (sizeBytes > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Avatar exceeds maximum size of 5 MB");
        }
        return sizeBytes;
    }

    private static String sanitizeFileName(String fileName, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }

        String baseName = fileName.trim()
                .replace('\\', '/')
                .replaceAll("^.*/", "");
        if (baseName.isBlank() || ".".equals(baseName) || "..".equals(baseName)) {
            throw new IllegalArgumentException("Invalid fileName");
        }

        String sanitized = baseName.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Invalid fileName");
        }

        String expectedExt = extensionForContentType(contentType);
        String lower = sanitized.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String actualExt = dot >= 0 ? lower.substring(dot + 1) : "";

        if (actualExt.isBlank()) {
            sanitized = sanitized + "." + expectedExt;
        } else if (!isExtensionAllowedForContentType(actualExt, contentType)) {
            throw new IllegalArgumentException(
                    "fileName extension does not match contentType: " + fileName);
        }

        if (sanitized.length() > 255) {
            throw new IllegalArgumentException("fileName is too long");
        }
        return sanitized;
    }

    private static boolean isExtensionAllowedForContentType(String extension, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg".equals(extension) || "jpeg".equals(extension);
            case "image/png" -> "png".equals(extension);
            case "image/webp" -> "webp".equals(extension);
            default -> false;
        };
    }

    private static String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported avatar contentType: " + contentType);
        };
    }
}
