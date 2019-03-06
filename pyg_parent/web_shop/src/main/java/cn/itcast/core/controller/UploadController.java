package cn.itcast.core.controller;

import cn.itcast.core.common.FastDFSClient;
import cn.itcast.core.pojo.entity.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.management.relation.RelationSupport;

@RestController
@RequestMapping("/upload")
public class UploadController {

    //根据config目录下的application.properties配置文件中的key获取value, 并且给fileServer全局变量赋值
    @Value("${FILE_SERVER_URL}")
    private String fileServer;

    /**
     * 上传文件
     * @param file 上传的文件对象, 接收的变量名必须等于页面提交的上传域的name属性值
     * @return
     */
    @RequestMapping("/uploadFile")
    public Result uploadFile(MultipartFile file) {
        try {
            //创建上传工具类对象
            FastDFSClient fastDFS = new FastDFSClient("classpath:fastDFS/fdfs_client.conf");
            //上传并返回上传后的文件路径和文件名   group1/M00/00/01/wKjIgFxiczKAP52sAAvqH_kipG8333.jpg
            String path = fastDFS.uploadFile(file.getBytes(), file.getOriginalFilename(), file.getSize());

            return new Result(true, fileServer + path);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "上传失败!");
        }
    }
}
