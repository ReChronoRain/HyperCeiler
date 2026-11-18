/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockWindowPolicy;
import java.util.Random;

public final class Golden {
    private static void emit(int width, int height, float density, int tall, int margin, int bottom, int radius) {
        var bounds = DockWindowPolicy.layout(width, height, density, tall, margin, bottom, radius);
        System.out.print(width + " " + height + " " + Float.floatToRawIntBits(density) + " "
                + tall + " " + margin + " " + bottom + " " + radius + " " + (bounds == null ? 0 : 1));
        if (bounds != null) System.out.print(" " + bounds.x() + " " + bounds.y() + " " + bounds.width()
                + " " + bounds.height() + " " + Float.floatToRawIntBits(bounds.radius()));
        System.out.println();
    }
    public static void main(String[] args) {
        int[] dimensions = {Integer.MIN_VALUE, -1, 0, 1, 2, 320, 1200, 2670, Integer.MAX_VALUE};
        float[] densities = {-1, -0f, 0f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.MIN_VALUE, .5f, 1, 2.75f, 3, 3.5f, Float.MAX_VALUE};
        for (int width : dimensions) for (int height : dimensions) for (float density : densities)
            emit(width, height, density, 150, 25, 15, 30);
        Random random = new Random(0x48434447);
        for (int i = 0; i < 20000; i++) emit(1 + random.nextInt(Integer.MAX_VALUE),
                1 + random.nextInt(Integer.MAX_VALUE), Float.intBitsToFloat(random.nextInt()),
                random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt());
        for (int i = 0; i < 10000; i++) emit(320 + random.nextInt(3000), 600 + random.nextInt(4000),
                random.nextInt(1601) / 160f, random.nextInt(350), random.nextInt(180),
                random.nextInt(180), random.nextInt(90));
    }
}
