package vn.edu.hust.soict.soe.assetmanagement.lookup.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LookupItemDto {
    private String id;
    private String code;
    private String name;
}
