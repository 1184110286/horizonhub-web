package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumBoardQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.mappers.ForumBoardMapper;
import com.nlizzard.horizonhub.service.ForumBoardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description:文章板块信息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("forumBoardService")
public class ForumBoardServiceImpl implements ForumBoardService {

    @Resource
    private ForumBoardMapper<ForumBoard, ForumBoardQuery> forumBoardMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumBoard> findListByParam(ForumBoardQuery query) {
        return this.forumBoardMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(ForumBoardQuery query) {
        return this.forumBoardMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<ForumBoard> findListByPage(ForumBoardQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ForumBoard> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumBoard bean) {
        return this.forumBoardMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumBoard> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumBoardMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumBoard> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumBoardMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据BoardId查询
     */
    @Override
    public ForumBoard getForumBoardByBoardId(Integer boardId) {
        return this.forumBoardMapper.selectByBoardId(boardId);
    }

    /**
     * 根据BoardId更新
     */
    @Override
    public Integer updateForumBoardByBoardId(ForumBoard bean, Integer boardId) {
        return this.forumBoardMapper.updateByBoardId(bean, boardId);
    }

    /**
     * 根据BoardId删除
     */
    @Override
    public Integer deleteForumBoardByBoardId(Integer boardId) {
        return this.forumBoardMapper.deleteByBoardId(boardId);
    }

    /**
     * 获取板块树
     *
     * @param postType 文章类型，1表示查询所有
     */
    @Override
    public List<ForumBoard> getBoardTree(Integer postType) {
        ForumBoardQuery forumBoardQuery = new ForumBoardQuery();
        forumBoardQuery.setOrderBy("sort ASC");
        forumBoardQuery.setPostType(postType);
        List<ForumBoard> forumBoardList = forumBoardMapper.selectList(forumBoardQuery);
        return convertLine2Tree(forumBoardList, 0);
    }

    // 原来递归时间复杂度O(n^2) 改良后时间复杂度O(n)
    private List<ForumBoard> convertLine2Tree(List<ForumBoard> forumBoardList, Integer rootId) {
        List<ForumBoard> tree = new ArrayList<>();
        // 建立 ID 与 对象的映射索引，利用引用传递
        Map<Integer, ForumBoard> map = forumBoardList.stream()
                .collect(Collectors.toMap(ForumBoard::getBoardId, node -> node));

        for (ForumBoard node : forumBoardList) {
            Integer pId = node.getPBoardId();
            if (pId.equals(rootId)) {
                // 如果是根节点，放入结果集
                tree.add(node);
            } else {
                // 如果不是根节点，直接通过索引找到父节点并挂载
                ForumBoard parent = map.get(pId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                }
            }
        }
        return tree;
    }


    /**
     * 新增 或更新论坛版块信息
     *
     * @param forumBoard 论坛版块信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveForumBoard(ForumBoard forumBoard) {
        // 如果boardId为空，表示新增，否则表示修改
        if (forumBoard.getBoardId() == null) {
            ForumBoardQuery boardQuery = new ForumBoardQuery();
            boardQuery.setPBoardId(forumBoard.getPBoardId());
            Integer count = this.forumBoardMapper.selectCount(boardQuery);
            // 板块排序默认为同级板块数量+1，即新增的板块排在最后
            forumBoard.setSort(count + 1);
            this.forumBoardMapper.insert(forumBoard);
        } else { // 修改板块信息
            ForumBoard dbInfo = this.forumBoardMapper.selectByBoardId(forumBoard.getBoardId());
            if (dbInfo == null) {
                throw new BusinessException("板块不存在");
            }
            this.forumBoardMapper.updateByBoardId(forumBoard, forumBoard.getBoardId());
            // 修改板块名称后需要同步更新文章表中的板块名称
            if (!dbInfo.getBoardName().equals(forumBoard.getBoardName())) {
                forumArticleMapper.updateBoardNameBatch(dbInfo.getPBoardId() == 0 ? 0 : 1, forumBoard.getBoardName(), forumBoard.getBoardId());
            }
        }
    }

    /**
     * 调整板块排序
     *
     * @param boardIds 逗号分隔的板块ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeSort(String boardIds) {
        String[] boardIdArray = boardIds.split(",");
        Integer index = 1;
        // 遍历板块ID列表，按照新的排序顺序更新每个板块的sort字段
        for (String boardIdStr : boardIdArray) {
            Integer boardId = Integer.parseInt(boardIdStr);
            ForumBoard board = new ForumBoard();
            board.setSort(index);
            forumBoardMapper.updateByBoardId(board, boardId);
            index++;
        }
    }
}