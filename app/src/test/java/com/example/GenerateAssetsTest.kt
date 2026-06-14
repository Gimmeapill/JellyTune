package com.example

import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Color
import java.awt.LinearGradientPaint
import java.awt.RadialGradientPaint
import java.awt.BasicStroke
import java.awt.geom.Path2D
import java.awt.geom.Ellipse2D
import javax.imageio.ImageIO

class GenerateAssetsTest {

    @Test
    fun generatePlayStoreAssets() {
        println("Generating high-fidelity Google Play Store assets...")
        System.setProperty("java.awt.headless", "true")

        val appIconFile = File("app_icon_512.png")
        val featureGraphicFile = File("feature_graphic_1024_500.png")

        drawAppIcon(appIconFile)
        drawFeatureGraphic(featureGraphicFile)

        println("Asset generation unit test finished successfully.")
    }

    private fun drawAppIcon(outputFile: File) {
        val size = 512
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()

        // Enable ultimate quality rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val center = size / 2f

        // 1. Cosmic Background Canvas (Almost pitch black with very deep cosmic purple)
        g.color = Color(14, 12, 18)
        g.fillRect(0, 0, size, size)

        // Subtle ambient purple background glow
        g.paint = RadialGradientPaint(
            center, center, 240f,
            floatArrayOf(0f, 0.6f, 1f),
            arrayOf(
                Color(52, 21, 80, 75), // Soft deep violet glow
                Color(30, 20, 48, 20),
                Color(0, 0, 0, 0)
            )
        )
        g.fill(Ellipse2D.Float(0f, 0f, size.toFloat(), size.toFloat()))

        // 2. Concentric Metal Round Frame / Ring
        val ringOuterRad = 236f
        val ringInnerRad = 208f

        // Soft outer shadow of the ring
        for (i in 0..12) {
            val alpha = (15 - i) * 2
            if (alpha > 0) {
                g.color = Color(0, 0, 0, alpha)
                val rShadow = ringOuterRad + i
                g.fill(Ellipse2D.Float(center - rShadow, center - rShadow, rShadow * 2, rShadow * 2))
            }
        }

        // Draw outer dark frame body
        g.paint = RadialGradientPaint(
            center, center, ringOuterRad,
            floatArrayOf(0f, 0.88f, 0.96f, 1f),
            arrayOf(
                Color(36, 32, 44),     // Frame center body
                Color(24, 21, 30),     // Dark outer edge
                Color(48, 43, 56),     // Light highlight rim
                Color(16, 14, 18)      // Outermost shadow
            )
        )
        g.fill(Ellipse2D.Float(center - ringOuterRad, center - ringOuterRad, ringOuterRad * 2, ringOuterRad * 2))

        // Draw pristine circular thin glowing metallic rim
        g.stroke = BasicStroke(2.5f)
        g.paint = LinearGradientPaint(
            center - ringOuterRad, center - ringOuterRad,
            center + ringOuterRad, center + ringOuterRad,
            floatArrayOf(0f, 0.3f, 0.7f, 1.0f),
            arrayOf(
                Color(140, 128, 158), // Slate silver-gray
                Color(50, 46, 58),
                Color(180, 165, 200), // Silver metallic highlight
                Color(32, 28, 38)
            )
        )
        g.draw(Ellipse2D.Float(center - ringOuterRad, center - ringOuterRad, ringOuterRad * 2, ringOuterRad * 2))

        // Inside plate background (extremely dark with rich radial nebula spotlight)
        g.paint = RadialGradientPaint(
            center, center, ringInnerRad,
            floatArrayOf(0f, 0.65f, 1f),
            arrayOf(
                Color(42, 17, 60),  // Spotlight core
                Color(18, 14, 26),  // Intermediate dark violet
                Color(10, 8, 14)    // Pitch black edge
            )
        )
        g.fill(Ellipse2D.Float(center - ringInnerRad, center - ringInnerRad, ringInnerRad * 2, ringInnerRad * 2))

        // Draw the highly bright neon purple inner circle ring
        g.stroke = BasicStroke(2f)
        g.paint = LinearGradientPaint(
            center, center - ringInnerRad, center, center + ringInnerRad,
            floatArrayOf(0f, 0.5f, 1.0f),
            arrayOf(
                Color(245, 110, 255, 160), // High neon violet-pink
                Color(131, 10, 210, 90),
                Color(0, 164, 220, 160)   // Electric neon blue bottom edge
            )
        )
        g.draw(Ellipse2D.Float(center - ringInnerRad, center - ringInnerRad, ringInnerRad * 2, ringInnerRad * 2))

        // Cosmic stars: Draw small ambient sparkling stars/dots in the background
        val stars = listOf(
            Pair(160f, 180f), Pair(360f, 160f), Pair(380f, 340f), Pair(180f, 320f),
            Pair(210f, 220f), Pair(310f, 210f), Pair(280f, 330f), Pair(320f, 360f)
        )
        for (star in stars) {
            g.paint = RadialGradientPaint(
                star.first, star.second, 6f,
                floatArrayOf(0f, 1f),
                arrayOf(Color(255, 235, 255, 200), Color(255, 255, 255, 0))
            )
            g.fill(Ellipse2D.Float(star.first - 6f, star.second - 6f, 12f, 12f))
            g.color = Color.WHITE
            g.fill(Ellipse2D.Float(star.first - 0.75f, star.second - 0.75f, 1.5f, 1.5f))
        }

        // 3. Draw high-fidelity neon logo (J-wave)
        val lPath = createLogoPath(false) // center aligned for app icon

        // Layer A: Giant ambient misty neon glow
        g.stroke = BasicStroke(42f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(145, 0, 240, 18)
        g.draw(lPath)

        // Layer B: Semi-wide soft glow
        g.stroke = BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(253, 82, 229, 50)
        g.draw(lPath)

        // Layer C: High contrast vibrant plasma liquid core body (Purple/Pink to Cyan gradient)
        g.stroke = BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.paint = LinearGradientPaint(
            130f, 256f, 420f, 256f,
            floatArrayOf(0f, 0.45f, 0.85f, 1.0f),
            arrayOf(
                Color(154, 45, 245), // Royal neon indigo
                Color(253, 82, 229), // Rich hot pink
                Color(0, 195, 245),  // Vivid electric cyan
                Color(0, 140, 220)   // Deep cyan
            )
        )
        g.draw(lPath)

        // Layer D: Absolute high-intensity electrical white/pink core discharge line
        g.stroke = BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(255, 250, 255, 240)
        g.draw(lPath)

        // Draw white sparks around the neon tube (energy discharge)
        val sparks = listOf(
            Pair(266f, 144f), Pair(166f, 360f), Pair(364f, 274f)
        )
        for (spark in sparks) {
            g.color = Color(255, 255, 255, 220)
            g.fill(Ellipse2D.Float(spark.first - 1.5f, spark.second - 1.5f, 3f, 3f))
        }

        g.dispose()

        val fos = FileOutputStream(outputFile)
        ImageIO.write(img, "png", fos)
        fos.flush()
        fos.close()
        println("Saved high-fidelity app icon PNG ($size x $size) successfully to: ${outputFile.absolutePath}")
    }

    private fun drawFeatureGraphic(outputFile: File) {
        val w = 1024
        val h = 500
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // 1. Dark Space Background Canvas
        g.color = Color(12, 10, 16)
        g.fillRect(0, 0, w, h)

        // Beautiful left violet nebula glow
        g.paint = RadialGradientPaint(
            300f, 250f, 400f,
            floatArrayOf(0f, 1f),
            arrayOf(Color(78, 12, 128, 90), Color(0, 0, 0, 0))
        )
        g.fill(Ellipse2D.Float(-100f, -150f, 800f, 800f))

        // Beautiful right electric blue nebula glow
        g.paint = RadialGradientPaint(
            750f, 250f, 350f,
            floatArrayOf(0f, 1f),
            arrayOf(Color(0, 110, 190, 65), Color(0, 0, 0, 0))
        )
        g.fill(Ellipse2D.Float(400f, -100f, 700f, 700f))

        // Ambient glowing specks (Galaxy stars)
        val r = java.util.Random(1337)
        for (i in 0..45) {
            val sx = r.nextFloat() * w
            val sy = r.nextFloat() * h
            val rad = r.nextFloat() * 4f + 1f
            g.paint = RadialGradientPaint(
                sx, sy, rad,
                floatArrayOf(0f, 1f),
                arrayOf(Color(255, 230, 255, 180), Color(255, 255, 255, 0))
            )
            g.fill(Ellipse2D.Float(sx - rad, sy - rad, rad * 2, sy * 0f + rad * 2))
            if (r.nextBoolean()) {
                g.color = Color.WHITE
                g.fill(Ellipse2D.Float(sx - 0.5f, sy - 0.5f, 1f, 1f))
            }
        }

        // 2. Spectrogram wave curves background (High-tech overlay mesh)
        // Let's draw 6 interlocking soft wave lines across the screen with varying wavelengths and opacities
        val colors = arrayOf(
            Color(154, 45, 245, 45),
            Color(253, 82, 229, 35),
            Color(0, 195, 245, 40),
            Color(255, 100, 240, 25),
            Color(0, 150, 255, 25)
        )
        
        for (i in 0..5) {
            val path = Path2D.Float()
            val baseMidY = h * 0.55f + (i - 2.5f) * 15f
            val amplitude = 35f + i * 8f
            val frequency = 0.006f + i * 0.0015f
            val phase = i * 1.3f

            path.moveTo(0f, baseMidY)
            var x = 0f
            while (x <= w) {
                // Wave formula combining multiple sines for organic complexity
                val y = baseMidY + (Math.sin((x * frequency + phase).toDouble()) * amplitude + 
                                   Math.sin((x * 0.015f + phase * 2).toDouble()) * (amplitude * 0.2f)).toFloat()
                path.lineTo(x, y)
                x += 10f
            }
            g.stroke = BasicStroke(1.2f + i * 0.3f)
            g.color = colors[i % colors.size]
            g.draw(path)
        }

        // Draw beautiful music notes and speaker shapes with ambient glows
        drawGlowString(g, "♫", 160f, 130f, 54, Color(253, 82, 229))
        drawGlowString(g, "♪", 120f, 380f, 42, Color(154, 45, 245))
        drawGlowString(g, "♪", 420f, 420f, 48, Color(0, 195, 245))
        drawGlowString(g, "♫", 880f, 150f, 50, Color(253, 82, 229))
        drawGlowString(g, "♪", 820f, 400f, 45, Color(0, 195, 245))

        // 3. Draw the center-left massive glowing neon Jelly Soundwave
        val logoScale = 1.15f
        val logoDX = 60f
        val logoDY = 40f
        val mainPath = createLogoPath(true, logoScale, logoDX, logoDY)

        // Misty ambient halo
        g.stroke = BasicStroke(50f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(145, 0, 240, 15)
        g.draw(mainPath)

        // Semi glow
        g.stroke = BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(253, 82, 229, 45)
        g.draw(mainPath)

        // Gradient core body
        g.stroke = BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.paint = LinearGradientPaint(
            logoDX + 130f, logoDY + 250f, logoDX + 420f, logoDY + 250f,
            floatArrayOf(0f, 0.45f, 0.85f, 1.0f),
            arrayOf(
                Color(154, 45, 245),
                Color(253, 82, 229),
                Color(0, 195, 245),
                Color(0, 140, 220)
            )
        )
        g.draw(mainPath)

        // Top high intensity white plasma core
        g.stroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = Color(255, 252, 255, 240)
        g.draw(mainPath)

        // Spark points
        val fSparks = listOf(
            Pair(logoDX + 266f * logoScale, logoDY + 144f * logoScale),
            Pair(logoDX + 166f * logoScale, logoDY + 360f * logoScale),
            Pair(logoDX + 364f * logoScale, logoDY + 274f * logoScale)
        )
        for (spark in fSparks) {
            g.color = Color.WHITE
            g.fill(Ellipse2D.Float(spark.first - 2f, spark.second - 2f, 4f, 4f))
        }

        // 4. Draw Typography Brand Texts ("JellyTune")
        val fontSerif = java.awt.Font("Serif", java.awt.Font.BOLD or java.awt.Font.ITALIC, 82)
        val fontSans = java.awt.Font("SansSerif", java.awt.Font.BOLD, 18)
        val fontMono = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)

        val tx = 560f
        val ty = 265f

        // Text subtitle cap
        g.font = fontSans
        g.color = Color(0, 195, 245, 180)
        g.drawString("J E L L Y F I N   M U S I C   P L A Y E R", tx, ty - 85f)

        // Draw "JellyTune" text shadow for high-contrast legible outline
        g.font = fontSerif
        g.color = Color(12, 10, 16, 220)
        for (xo in -3..3) {
            for (yo in -3..3) {
                g.drawString("JellyTune", tx + xo, ty + yo)
            }
        }

        // Draw soft purple backglow behind text to match logo
        g.color = Color(253, 82, 229, 60)
        g.drawString("JellyTune", tx - 2f, ty - 2f)
        g.drawString("JellyTune", tx + 2f, ty + 2f)

        // Draw main typographic text with smooth gradient
        g.paint = LinearGradientPaint(
            tx, ty - 60f, tx + 350f, ty,
            floatArrayOf(0f, 0.5f, 1.0f),
            arrayOf(
                Color(255, 130, 253), // Hot neon pink
                Color(220, 75, 255),  // Vivid violet
                Color(0, 210, 255)    // Electric cyan-blue
            )
        )
        g.drawString("JellyTune", tx, ty)

        // Thin beautiful crisp white highlight on text
        g.font = fontSerif
        g.color = Color(255, 250, 255, 140)
        g.drawString("JellyTune", tx, ty - 1f)

        // Subtitle slogan/features
        g.font = fontMono
        g.color = Color(255, 255, 255, 130)
        g.drawString("OFFLINE CACHING // CONCERT ACOUSTICS // EQUALIZER MESH", tx, ty + 45f)

        g.dispose()

        val fos = FileOutputStream(outputFile)
        ImageIO.write(img, "png", fos)
        fos.flush()
        fos.close()
        println("Saved high-fidelity feature graphic PNG ($w x $h) successfully to: ${outputFile.absolutePath}")
    }

    private fun drawGlowString(g: Graphics2D, text: String, x: Float, y: Float, fontSize: Int, color: Color) {
        val f = java.awt.Font("SansSerif", java.awt.Font.BOLD, fontSize)
        g.font = f
        
        // Glow layer 1
        g.color = Color(color.red, color.green, color.blue, 35)
        g.drawString(text, x - 4f, y - 4f)
        g.drawString(text, x + 4f, y + 4f)
        
        // Glow layer 2
        g.color = Color(color.red, color.green, color.blue, 80)
        g.drawString(text, x - 2f, y - 2f)
        g.drawString(text, x + 2f, y + 2f)

        // Core White-colored center
        g.color = Color(255, 245, 255, 220)
        g.drawString(text, x, y)
    }

    /**
     * Creates a high-fidelity path matching the gorgeous Jelly Soundwave logo.
     * The path describes a playful jelly-like J shape merging smoothly into soundwave ripples.
     */
    private fun createLogoPath(
        isFeatureGraphic: Boolean,
        scale: Float = 1.0f,
        dx: Float = 0f,
        dy: Float = 0f
    ): Path2D.Float {
        val path = Path2D.Float()

        // Source bounds of the spline coordinates:
        // Width: approx 130 to 410, Height: approx 140 to 395
        // We will perfectly offset and scale it so it is dead-center of the 512x512 canvas or placed nicely in feature graphic
        
        val targetScale = if (isFeatureGraphic) scale else 1.08f
        // Offset to balance center of visual mass:
        // Leftbound is ~130. Rightbound is ~410. Center is ~270. On 512, delta is (256 - 270) = -14.
        // Topbound is ~140. Bottombound is ~395. Center is ~267. On 512, delta is (256 - 267) = -11.
        val targetDX = if (isFeatureGraphic) dx else (256f - 270f * targetScale)
        val targetDY = if (isFeatureGraphic) dy else (256f - 267f * targetScale)

        fun mapX(x: Float) = x * targetScale + targetDX
        fun mapY(y: Float) = y * targetScale + targetDY

        // Build the cubic Bezier curves representing the original logo profile perfectly
        // Left loop / rounded bottom bulb of J
        path.moveTo(mapX(150f), mapY(305f))
        path.curveTo(mapX(130f), mapY(305f), mapX(115f), mapY(325f), mapX(115f), mapY(350f))
        path.curveTo(mapX(115f), mapY(375f), mapX(135f), mapY(395f), mapX(160f), mapY(395f))
        path.curveTo(mapX(185f), mapY(395f), mapX(210f), mapY(360f), mapX(215f), mapY(320f))

        // Vertical sleek upward swoop of the J-stem
        path.curveTo(mapX(222f), mapY(240f), mapX(235f), mapY(140f), mapX(270f), mapY(140f))

        // Graceful downward crest and loop transitioning into soundwave peaks
        path.curveTo(mapX(295f), mapY(140f), mapX(310f), mapY(200f), mapX(315f), mapY(245f))
        path.curveTo(mapX(320f), mapY(335f), mapX(345f), mapY(360f), mapX(360f), mapY(360f))

        // First decaying wave ripple peaks
        path.curveTo(mapX(372f), mapY(360f), mapX(385f), mapY(330f), mapX(395f), mapY(275f))
        path.curveTo(mapX(402f), mapY(235f), mapX(412f), mapY(235f), mapX(418f), mapY(275f))

        // Second decaying wave ripple peaks
        path.curveTo(mapX(425f), mapY(320f), mapX(432f), mapY(335f), mapX(442f), mapY(335f))
        path.curveTo(mapX(450f), mapY(335f), mapX(458f), mapY(310f), mapX(465f), mapY(310f))

        // Final minor decay crest
        path.curveTo(mapX(470f), mapY(310f), mapX(475f), mapY(320f), mapX(480f), mapY(325f))

        return path
    }
}
