package com.feishu.miji.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音频处理工具类
 */
@Slf4j
@UtilityClass
public class AudioUtils {
    
    /**
     * PCM 数据转换为 WAV 格式
     * 
     * @param pcmData PCM 原始数据
     * @param audioFormat 音频格式
     * @return WAV 格式数据
     */
    public static byte[] pcmToWav(byte[] pcmData, AudioFormat audioFormat) throws IOException {
        int totalAudioLen = pcmData.length;
        int totalDataLen = totalAudioLen + 36;
        int channels = audioFormat.getChannels();
        int sampleRate = (int) audioFormat.getSampleRate();
        int byteRate = sampleRate * channels * audioFormat.getSampleSizeInBits() / 8;
        int bitsPerSample = audioFormat.getSampleSizeInBits();
        int blockAlign = channels * bitsPerSample / 8;

        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);

        header.put("RIFF".getBytes());
        header.putInt(totalDataLen);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put("data".getBytes());
        header.putInt(totalAudioLen);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.array());
        out.write(pcmData);

        return out.toByteArray();
    }
    
    /**
     * 创建标准音频格式（16位 16kHz 单声道）
     */
    public static AudioFormat createStandardFormat() {
        float sampleRate = 16000f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    
    /**
     * 获取音频时长（秒）
     */
    public static float getDuration(byte[] audioData, AudioFormat format) {
        return (float) audioData.length / (format.getSampleRate() * format.getChannels() * format.getSampleSizeInBits() / 8);
    }
    
    /**
     * 验证音频格式是否支持
     */
    public static boolean isFormatSupported(AudioFormat format) {
        float sampleRate = format.getSampleRate();
        return (sampleRate >= 8000 && sampleRate <= 48000);
    }
}
