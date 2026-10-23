package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.service.ForumBoardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/board")
public class ForumBoardController extends BaseController {

    @Resource
    private ForumBoardService forumBoardService;

    /**
     * 加载板块树
     *
     * @return
     */
    @RequestMapping("/loadBoard")
    public ResponseVO<List<ForumBoard>> loadBoard() {
        return getSuccessResponseVO(forumBoardService.getBoardTree(null));
    }

}
