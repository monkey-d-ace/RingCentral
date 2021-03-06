package com.RingCentral.controller;

import com.RingCentral.mapper.FilePathMapper;
import com.RingCentral.model.FilePath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RestController
public class Controller {
    @Value("${filepath}")
    private String filepath;
    private ConcurrentHashMap<String, ReentrantLock> concurrentHashMap = new ConcurrentHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock(true);
    
    @Autowired
    private FilePathMapper filePathMapper;

    @PostMapping("/create")
    public void create(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (Files.notExists(Paths.get(filepath)))
                Files.createDirectories(Paths.get(filepath));
            String info = request.getParameter("info");
            String name = request.getParameter("name");
            Path path = Paths.get(filepath + name + ".txt");
            byte[] bytes = info.getBytes();
            Files.write(path, bytes);
            System.out.println(filepath);
            FilePath filePath = new FilePath();
            filePath.setName(name);
            filePathMapper.insert(filePath);
            response.sendRedirect("/view");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/view")
    public ModelAndView view() {
        return new ModelAndView("view");
    }

    @GetMapping("/list")
    public Map<String, List<FilePath>> list() {
        Map<String, List<FilePath>> map = new HashMap<>();
        map.put("data", filePathMapper.selectAll());
        return map;
    }

    @GetMapping("/download/{name}")
    public void download(@PathVariable("name") String name, HttpServletResponse response) throws IOException {
        System.out.println(name);
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".txt");
        Path path = Paths.get(filepath + name + ".txt");
        InputStream inputStream = new FileSystemResource(filepath + name + ".txt").getInputStream();
        int i;
        OutputStream outputStream = response.getOutputStream();
        while ((i = inputStream.read()) != -1) {
            outputStream.write(i);
        }
        inputStream.close();
        outputStream.close();
    }

    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") String id, HttpServletRequest request) throws IOException {
        FilePath filePath = filePathMapper.selectByPrimaryKey(id);
        ModelAndView modelAndView = new ModelAndView("edit");
        modelAndView.addObject("filePath", filePath);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InputStream inputStream = new FileSystemResource(filepath + filePath.getName() + ".txt").getInputStream();
        int i;
        while ((i = inputStream.read()) != -1) {
            byteArrayOutputStream.write(i);
        }
        if (request.getParameter("wait") != null) {
            modelAndView.addObject("wait", true);
        }
        modelAndView.addObject("info", byteArrayOutputStream.toString());
        return modelAndView;
    }

    @PostMapping("/update/{id}")
    public void update(@PathVariable("id") String id, @RequestParam("info") String info, HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println(id);
        if (!concurrentHashMap.containsKey(id)) {
            concurrentHashMap.put(id, new ReentrantLock(true));
        }
        ReentrantLock lock = concurrentHashMap.get(id);

        if (lock.tryLock()) {
            try {
                FilePath filePath = filePathMapper.selectByPrimaryKey(id);
                String name = filePath.getName();
                Path path = Paths.get(filepath + name + ".txt");
                byte[] bytes = info.getBytes();
                Files.write(path, bytes);
                TimeUnit.SECONDS.sleep(60);
                response.sendRedirect("/edit/" + id);
            } catch (InterruptedException e) {
                System.out.println("线程中断");
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("-------------------------");
            response.sendRedirect("/edit/" + id + "?wait=true");
        }
        /*
        try {
            if (lock.tryLock()) {
                FilePath filePath = filePathMapper.selectByPrimaryKey(id);
                String name = filePath.getName();
                Path path = Paths.get(filepath + name + ".txt");
                byte[] bytes = info.getBytes();
                Files.write(path, bytes);
                TimeUnit.SECONDS.sleep(60);
                response.sendRedirect("/edit/" + id);
                lock.unlock();
            } else {
                System.out.println("-------------------------");
                response.sendRedirect("/edit/" + id + "?wait=true");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }
}
