package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:文件信息Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumArticleAttachmentMapper<T, P> extends BaseMapper<T, P> {
    /**
     * 根据FileId查询
     */
    T selectByFileId(@Param("fileId") String fileId);

    /**
     * 根据FileId更新
     */
    Integer updateByFileId(@Param("bean") T t, @Param("fileId") String fileId);

    /**
     * 根据FileId删除
     */
    Integer deleteByFileId(@Param("fileId") String fileId);

    /**
     * 根据FileId更新下载次数
     */
    void updateDownloadCount(@Param("fileId") String fileId);
}