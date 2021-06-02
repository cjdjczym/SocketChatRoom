package client;

import java.io.*;

/**
 * 将.pcm文件转为WAV文件
 */
public class Pcm2Wav {
    public static void convertAudioFiles(String[] src) throws IOException {
        FileInputStream fis = new FileInputStream(src[0]);

        //计算长度
        byte[] buf = new byte[1024 * 4];
        int size = fis.read(buf);
        int PCMSize = 0;
        while (size != -1) {
            PCMSize += size;
            size = fis.read(buf);
        }
        fis.close();

        //填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = PCMSize + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 2;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 44100;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = PCMSize;

        byte[] h = header.getHeader();
        assert h.length == 44; //WAV标准，头部应该是44字节

        FileOutputStream fs = new FileOutputStream(src[1]);
        fs.write(h);
        FileInputStream fiss = new FileInputStream(src[0]);
        byte[] bb = new byte[10];
        int len;
        while ((len = fiss.read(bb)) > 0) {
            fs.write(bb, 0, len);
        }
        fs.close();
        fiss.close();
    }

    /**
     * WavHeader辅助类。用于生成头部信息。
     */
    static class WaveHeader {
        public final char[] fileID = {'R', 'I', 'F', 'F'};
        public int fileLength;
        public short FormatTag;
        public short Channels;
        public int SamplesPerSec;
        public int AvgBytesPerSec;
        public short BlockAlign;
        public short BitsPerSample;
        public char[] DataHdrID = {'d', 'a', 't', 'a'};
        public int DataHdrLeth;
        public char[] wavTag = {'W', 'A', 'V', 'E'};
        public char[] FmtHdrID = {'f', 'm', 't', ' '};
        public int FmtHdrLeth;

        public byte[] getHeader() throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            WriteChar(bos, fileID);
            WriteInt(bos, fileLength);
            WriteChar(bos, wavTag);
            WriteChar(bos, FmtHdrID);
            WriteInt(bos, FmtHdrLeth);
            WriteShort(bos, FormatTag);
            WriteShort(bos, Channels);
            WriteInt(bos, SamplesPerSec);
            WriteInt(bos, AvgBytesPerSec);
            WriteShort(bos, BlockAlign);
            WriteShort(bos, BitsPerSample);
            WriteChar(bos, DataHdrID);
            WriteInt(bos, DataHdrLeth);
            bos.flush();
            byte[] r = bos.toByteArray();
            bos.close();
            return r;
        }

        private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
            byte[] myByte = new byte[2];
            myByte[1] = (byte) ((s << 16) >> 24);
            myByte[0] = (byte) ((s << 24) >> 24);
            bos.write(myByte);
        }


        private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
            byte[] buf = new byte[4];
            buf[3] = (byte) (n >> 24);
            buf[2] = (byte) ((n << 8) >> 24);
            buf[1] = (byte) ((n << 16) >> 24);
            buf[0] = (byte) ((n << 24) >> 24);
            bos.write(buf);
        }

        private void WriteChar(ByteArrayOutputStream bos, char[] id) {
            for (char c : id) {
                bos.write(c);
            }
        }
    }
}