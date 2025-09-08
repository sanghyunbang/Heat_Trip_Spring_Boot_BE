package com.heattrip.heat_trip_backend.S3.media;

import com.heattrip.heat_trip_backend.S3.UploadCategory;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * 업로드된 파일의 메타데이터 엔티티.
 * - DB에는 S3 key를 저장(CloudFront URL은 런타임 조립)
 */
@Entity
@Table(name = "media_objects")
public class MediaObject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadCategory category;

    @Column(nullable = false)
    private String ownerId;             // 사용자 ID

    private String refType;             // 연결 타입 (JOURNEY/REVIEW/PROFILE 등)
    private String refId;               // 연결 ID   (예: journeyId, reviewId)

    @Column(nullable = false, unique = true, length = 512)
    private String key;                 // S3 key

    private String originalName;
    private String contentType;
    private long size;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
    private Instant deletedAt;          // 소프트 삭제용 (옵션)

    @PreUpdate void touch(){ updatedAt = Instant.now(); }

    public static MediaObject of(UploadCategory cat, String ownerId, String refType, String refId,
                                 String key, String originalName, String contentType, long size){
        MediaObject m = new MediaObject();
        m.category = cat; m.ownerId = ownerId; m.refType = refType; m.refId = refId;
        m.key = key; m.originalName = originalName; m.contentType = contentType; m.size = size;
        return m;
    }

    public MediaObject() {}

    // getters/setters (lombok 사용 시 @Getter/@Setter로 대체 가능)
    public Long getId(){return id;}
    public UploadCategory getCategory(){return category;}
    public void setCategory(UploadCategory c){this.category=c;}
    public String getOwnerId(){return ownerId;}
    public void setOwnerId(String s){this.ownerId=s;}
    public String getRefType(){return refType;}
    public void setRefType(String r){this.refType=r;}
    public String getRefId(){return refId;}
    public void setRefId(String r){this.refId=r;}
    public String getKey(){return key;}
    public void setKey(String k){this.key=k;}
    public String getOriginalName(){return originalName;}
    public void setOriginalName(String o){this.originalName=o;}
    public String getContentType(){return contentType;}
    public void setContentType(String c){this.contentType=c;}
    public long getSize(){return size;}
    public void setSize(long s){this.size=s;}
    public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;}
    public Instant getDeletedAt(){return deletedAt;}
    public void setDeletedAt(Instant d){this.deletedAt=d;}
}
