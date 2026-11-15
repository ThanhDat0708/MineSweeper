package sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Tạo và phát âm thanh tổng hợp bằng javax.sound.sampled (không cần file âm thanh
 * hay thư viện ngoài). Âm thanh chạy trên luồng nền để không chặn EDT.
 */
public final class SoundManager {

    private static final AudioFormat FORMAT =
            new AudioFormat(44100f, 16, 1, true, false);
    private static final int SAMPLE_RATE = (int) FORMAT.getSampleRate();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "minesweeper-sound");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean enabled = true;
    private volatile long lastRevealAt;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void playReveal() {
        if (!enabled) {
            return;
        }
        long now = System.currentTimeMillis();
        // Bỏ qua nếu vừa phát để tránh dồn tiếng khi mở ô liên tiếp.
        if (now - lastRevealAt < 35) {
            return;
        }
        lastRevealAt = now;
        enqueue(() -> tone(720, 45, 0.18));
    }

    public void playFlag() {
        if (!enabled) {
            return;
        }
        enqueue(() -> tone(1050, 35, 0.16));
    }

    public void playExplosion() {
        if (!enabled) {
            return;
        }
        enqueue(SoundManager::explosion);
    }

    public void playAlert() {
        if (!enabled) {
            return;
        }
        enqueue(() -> sequence(new double[]{330.0, 262.0}, 90, 40, 0.2));
    }

    public void playDefuse() {
        if (!enabled) {
            return;
        }
        enqueue(() -> sequence(new double[]{660.0, 880.0, 1100.0}, 70, 20, 0.2));
    }

    public void playWin() {
        if (!enabled) {
            return;
        }
        enqueue(SoundManager::winMelody);
    }

    public void dispose() {
        executor.shutdownNow();
    }

    private void enqueue(Supplier<byte[]> generator) {
        executor.submit(() -> {
            try {
                playRaw(generator.get());
            } catch (LineUnavailableException | IllegalArgumentException ex) {
                // Máy không có thiết bị âm thanh: bỏ qua để game vẫn chạy bình thường.
            }
        });
    }

    private static void playRaw(byte[] pcm) throws LineUnavailableException {
        if (pcm.length == 0) {
            return;
        }
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
            line.stop();
        }
    }

    private static byte[] tone(double frequency, int durationMs, double volume) {
        int samples = Math.max(1, SAMPLE_RATE * durationMs / 1000);
        byte[] data = new byte[samples * 2];
        int attack = Math.max(1, SAMPLE_RATE * 5 / 1000);
        int release = Math.max(1, SAMPLE_RATE * 18 / 1000);

        for (int i = 0; i < samples; i++) {
            double envelope = 1.0;
            if (i < attack) {
                envelope = (double) i / attack;
            }
            int remaining = samples - i;
            if (remaining < release) {
                envelope = Math.min(envelope, (double) remaining / release);
            }
            double sample = Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE)
                    * volume * envelope;
            putSample(data, i, sample);
        }
        return data;
    }

    private static byte[] winMelody() {
        // Đô - Mi - Sol - Đô (quãng tám trên): giai điệu chiến thắng ngắn.
        double[] notes = {523.25, 659.25, 783.99, 1046.50};
        return sequence(notes, 120, 30, 0.22);
    }

    private static byte[] sequence(double[] frequencies, int noteMs, int gapMs, double volume) {
        int noteBytes = tone(440, noteMs, 0).length;
        int gapBytes = SAMPLE_RATE * gapMs / 1000 * 2;
        int total = frequencies.length * noteBytes + (frequencies.length - 1) * gapBytes;
        byte[] data = new byte[total];
        int offset = 0;
        for (int n = 0; n < frequencies.length; n++) {
            byte[] note = tone(frequencies[n], noteMs, volume);
            System.arraycopy(note, 0, data, offset, note.length);
            offset += note.length;
            if (n < frequencies.length - 1) {
                offset += gapBytes; // khoảng lặng giữa các nốt
            }
        }
        return data;
    }

    private static byte[] explosion() {
        int samples = Math.max(1, SAMPLE_RATE * 500 / 1000);
        byte[] data = new byte[samples * 2];
        Random random = new Random();
        double filtered = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            double envelope = Math.pow(1.0 - progress, 1.6);
            double noise = random.nextDouble() * 2.0 - 1.0;
            filtered = filtered * 0.75 + noise * 0.25;
            double rumble = Math.sin(2.0 * Math.PI * 65.0 * t) * 0.55;
            double sample = (filtered * 0.75 + rumble) * envelope * 0.85;
            putSample(data, i, sample);
        }
        return data;
    }

    private static void putSample(byte[] data, int index, double sample) {
        double clamped = Math.max(-1.0, Math.min(1.0, sample));
        short value = (short) Math.round(clamped * Short.MAX_VALUE);
        data[index * 2] = (byte) (value & 0xFF);
        data[index * 2 + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
