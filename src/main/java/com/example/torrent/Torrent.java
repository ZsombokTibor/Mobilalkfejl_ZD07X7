package com.example.torrent;

public class Torrent {
    private String id;
    private String name;
    private String code;
    private int progress;


    public Torrent() {
        this.progress = 0;
    }

    public Torrent(String name, String code) {
        this.name = name;
        this.code = code;
        this.progress = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
} 