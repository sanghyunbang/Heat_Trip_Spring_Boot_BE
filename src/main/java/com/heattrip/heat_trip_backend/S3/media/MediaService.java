package com.heattrip.heat_trip_backend.S3.media;

import com.heattrip.heat_trip_backend.S3.*;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 미디어 도메인 서비스.
 * - 저장소(FileStorage) + JPA 리포지토리 결합
 * - 권한/연결(refType/refId) 처리
 */
@Service
public class MediaService {

    private final FileStorage storage;
    private final MediaObjectRepository repo;

    public MediaService(FileStorage storage, MediaObjectRepository repo) {
        this.storage = storage;
        this.repo = repo;
    }

    /** C: 여러 장 업로드 */
    @Transactional
    public List<MediaObject> uploadMany(List<MultipartFile> files, UploadCategory cat,
                                        String ownerId, String refType, String refId) {
        return files.stream()
            .filter(f -> f != null && !f.isEmpty())
            .map(f -> {
                StoredObject so = storage.upload(f, UploadRequest.of(cat, ownerId).withSubPath(refId));
                MediaObject m = MediaObject.of(cat, ownerId, refType, refId,
                        so.key(), f.getOriginalFilename(), f.getContentType(), f.getSize());
                return repo.save(m);
            })
            .collect(Collectors.toList()); // JDK < 16 호환
    }

    /** R: 공개 URL */
    public String publicUrl(String key) {
        return storage.publicUrl(key);
    }

    /** R: 프리사인드 URL */
    public String presignedUrl(String key, Duration ttl) {
        return storage.presignedUrl(key, ttl).toString();
    }

    /** R: refType/refId로 이미지 목록 조회 (컨트롤러에서 사용) */
    @Transactional(readOnly = true)
    public List<MediaObject> findByRef(String refType, String refId) {
        return repo.findByRefTypeAndRefId(refType, refId);
    }

    /** U: 교체 (새 업로드 → 구 키 삭제 → 메타 갱신) */
    @Transactional
    public MediaObject replace(Long mediaId, MultipartFile file, String ownerId) {
        MediaObject m = repo.findById(mediaId).orElseThrow();
        if (!m.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("권한 없음");

        StoredObject neo = storage.replace(m.getKey(), file,
                UploadRequest.of(m.getCategory(), ownerId).withSubPath(m.getRefId()));

        m.setKey(neo.key());
        m.setContentType(neo.contentType());
        m.setSize(neo.size());
        return repo.save(m);
    }

    /** D: 단건 삭제 (owner 검증) */
    @Transactional
    public void delete(Long mediaId, String ownerId) {
        MediaObject m = repo.findById(mediaId).orElseThrow();
        if (!m.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("권한 없음");
        storage.delete(m.getKey());
        repo.delete(m);
    }

    /** D: 단건 삭제 (여정과의 연결 검증까지 수행) */
    @Transactional
    public void deleteForJourney(Long mediaId, String journeyId, String ownerId) {
        MediaObject m = repo.findById(mediaId).orElseThrow();
        if (!m.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("권한 없음");
        if (!"JOURNEY".equals(m.getRefType()) || !journeyId.equals(m.getRefId())) {
            throw new IllegalArgumentException("잘못된 참조(journeyId 불일치)");
        }
        storage.delete(m.getKey());
        repo.delete(m);
    }

    /** D: 연결 리소스 전체 삭제 (예: 특정 여정의 모든 이미지) */
    @Transactional
    public void deleteByRef(String refType, String refId, String ownerId) {
        var list = repo.findByRefTypeAndRefId(refType, refId);
        var own = list.stream().filter(m -> ownerId.equals(m.getOwnerId())).collect(Collectors.toList());
        storage.deleteAll(own.stream().map(MediaObject::getKey).collect(Collectors.toList()));
        repo.deleteAll(own);
    }
}
