package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.query.ForumBoardQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:文章板块信息Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumBoardService {

    /**
     * 根据条件查询列表
     */
    List<ForumBoard> findListByParam(ForumBoardQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(ForumBoardQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumBoard> findListByPage(ForumBoardQuery query);

    /**
     * 新增
     */
    Integer add(ForumBoard bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumBoard> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<ForumBoard> listBean);

    /**
     * 根据BoardId查询
     */
    ForumBoard getForumBoardByBoardId(Integer boardId);

    /**
     * 根据BoardId更新
     */
    Integer updateForumBoardByBoardId(ForumBoard bean, Integer boardId);

    /**
     * 根据BoardId删除
     */
    Integer deleteForumBoardByBoardId(Integer boardId);

    /**
     * 获取板块树
     *
     * @param postType 文章类型，0:管理员可发帖 1:所有人可发帖，null:所有板块
     */
    List<ForumBoard> getBoardTree(Integer postType);

    /**
     * 新增 或更新论坛版块信息
     *
     * @param forumBoard 论坛版块信息
     */
    void saveForumBoard(ForumBoard forumBoard);

    /**
     * 修改板块的优先级
     *
     * @param boardIds 逗号分隔的板块ID列表
     */
    void changeSort(String boardIds);
}