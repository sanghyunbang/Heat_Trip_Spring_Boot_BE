package com.heattrip.heat_trip_backend.curation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사용자 PAD 입력(-2,-1,1,2) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PadDTO {
    private int pleasure;
    private int arousal;
    private int dominance;
}
