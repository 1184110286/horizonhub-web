package com.nlizzard.horizonhub.controller;


import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.FileUploadDto;
import com.nlizzard.horizonhub.entity.enums.FileUploadTypeEnum;
import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.service.ForumBoardService;
import com.nlizzard.horizonhub.utils.FileUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/board")
public class ForumBoardController extends BaseController {

    @Resource
    private ForumBoardService forumBoardService;

    @Resource
    private FileUtils fileUtils;

    /**
     * 加载论坛版块树
     */
    @RequestMapping("/loadBoard")
    public ResponseVO<List<ForumBoard>> loadBoard() {
        return getSuccessResponseVO(forumBoardService.getBoardTree(null));
    }

    /**
     * 新增或修改板块信息
     *
     * @param boardId   版块 ID，修改时传入，新增时不传
     * @param pBoardId  父版块 ID，一级版块传入 0
     * @param boardName 版块名称
     * @param boardDesc 版块描述
     * @param postType  发帖类型，0-普通发帖，1-投票发帖
     * @param cover     版块封面图片
     */
    @PostMapping("/saveBoard")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> saveBoard(Integer boardId,
                                      @VerifyParam(required = true) Integer pBoardId,
                                      @VerifyParam(required = true) String boardName,
                                      String boardDesc,
                                      Integer postType,
                                      MultipartFile cover) {
        ForumBoard forumBoard = new ForumBoard();
        forumBoard.setBoardId(boardId);
        forumBoard.setPBoardId(pBoardId);
        forumBoard.setBoardName(boardName);
        forumBoard.setBoardDesc(boardDesc);
        forumBoard.setPostType(postType);
        // 如果上传了封面图片，先上传图片再保存版块信息
        if (cover != null) {
            FileUploadDto uploadDto = fileUtils.uploadFile2Local(cover, FileUploadTypeEnum.ARTICLE_COVER, Constants.FILE_FOLDER_IMAGE);
            forumBoard.setCover(uploadDto.getLocalPath());
        }
        // 更新或新增版块信息
        forumBoardService.saveForumBoard(forumBoard);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除板块信息
     *
     * @param boardId 版块 ID
     */
    @PostMapping("/delBoard")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> delBoard(@VerifyParam(required = true) Integer boardId) {
        forumBoardService.deleteForumBoardByBoardId(boardId);
        return getSuccessResponseVO(null);
    }

    /**
     * 修改板块排序
     *
     * @param boardIds 逗号分隔的板块 ID 列表，按照新的排序顺序传入
     */
    @PostMapping("/changeBoardSort")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> changeSort(@VerifyParam(required = true) String boardIds) {
        forumBoardService.changeSort(boardIds);
        return getSuccessResponseVO(null);
    }
}
