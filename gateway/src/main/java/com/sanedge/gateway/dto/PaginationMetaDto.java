package com.sanedge.gateway.dto;

public record PaginationMetaDto(
    int currentPage,
    int pageSize,
    int totalPages,
    int totalRecords
) {
    public static PaginationMetaDto from(pb.common.PaginationMeta proto) {
        return new PaginationMetaDto(
            proto.getCurrentPage(),
            proto.getPageSize(),
            proto.getTotalPages(),
            proto.getTotalRecords()
        );
    }
}
