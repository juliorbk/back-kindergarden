package com.ueclca.backend.services;

public interface TelegramClient {

    String sendPhoto(String photoUrl, String caption);

    String sendPhoto(byte[] content, String filename, String contentType, String caption);

    String sendVideo(String videoUrl, String caption);

    String sendVideo(byte[] content, String filename, String contentType, String caption);

    String sendMessage(String text);
}
