package vip.comic18.finder.runner;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vip.comic18.finder.entity.ComicEntity;
import vip.comic18.finder.service.ComicService;

import java.util.List;

/**
 * Created by jiayao on 2021/2/16.
 */
@Component
@Slf4j
public class ComicRunner implements CommandLineRunner {
    @Autowired
    private ComicService comicService;

    private final List<String> comicHomePages = JSONUtil.toList(new ClassPathResource("downloadPath.json").readUtf8Str(), String.class);

    @Override
    public void run(String... args) {
        log.info("注意身体,适度看漫");
        if(ArrayUtil.contains(args, "-s")) {
            log.info("运行模式 : 后台模式");
            return;
        }
        log.info("运行模式 : 前台模式");
        ClassPathResource classPathResource = new ClassPathResource("downloadPath.json");
        classPathResource.readUtf8Str();
        if(CollUtil.isEmpty(comicHomePages)) {
            log.info("下载列表为空,终止任务");
            HttpUtil.createPost("http://localhost:7789/actuator/shutdown").contentType(ContentType.JSON.getValue()).execute();
            return;
        }
        comicHomePages.forEach(comicHomePage -> {
            ComicEntity comicInfo = comicService.getComicInfo(comicHomePage);
            log.info("开始下载[{}]:[{}]", comicInfo.getTitle(), comicHomePage);
            comicService.downloadComic(comicInfo);
            log.info("完成下载[{}]:[{}]", comicInfo.getTitle(), comicHomePage);
        });
        while(DateUtil.date().isBefore(DateUtil.offsetSecond(FileUtil.lastModifiedTime(FileUtil.file(SystemUtil.get(SystemUtil.USER_DIR) + "/logs/18-comic-finder/finder-info.log")), 30))) {
            ThreadUtil.sleep(60000L);
        }
        log.info("下载完成,终止任务");
        HttpUtil.createPost("http://localhost:7789/actuator/shutdown").contentType(ContentType.JSON.getValue()).execute();
    }
}
