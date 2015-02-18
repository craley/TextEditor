/**
 *
 *
 *
 */
package realdeal;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author chris
 */
public class Metrics {

    int ascent;
    int descent;
    int advance;
    int textheight;
    float preciseWidth;
    FontMetrics fm;
    //contains the width of each char
    Map<Character, Integer> charmap = new HashMap<>();

    public Metrics() {
    }
    public Metrics(Font font){
        BufferedImage bi = new BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g2 = (Graphics2D) (bi.createGraphics());
        FontRenderContext fcxt = g2.getFontRenderContext();
        //linemetrics creates a more accurate value(uses floats)
        LineMetrics lmetrics = font.getLineMetrics("sample text", fcxt);
        ascent = (int) lmetrics.getAscent();
        descent = (int) lmetrics.getDescent();
        textheight = (int) lmetrics.getHeight();
        fm = g2.getFontMetrics(font);
        advance = fm.charWidth('t') + 1;//fm.getMaxAdvance(); say 21!!!
        preciseWidth = ((int)(advance * 1.2));
        //loop thru usable ascii chars: 32(space) to 126()
        for (int p = 32; p < 127; p++) {
            charmap.put((char) p, fm.charWidth(p));
        }
        charmap.put('\n', fm.charWidth('m'));//not really needed anymore
    }

    //static constructor
    public static Metrics buildMetrics(Font font) {
        Metrics m = new Metrics();
        BufferedImage bi = new BufferedImage(2, 2, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g2 = (Graphics2D) (bi.createGraphics());
        FontRenderContext fcxt = g2.getFontRenderContext();
        //linemetrics creates a more accurate value(uses floats)
        LineMetrics lmetrics = font.getLineMetrics("sample text", fcxt);
        m.ascent = (int) lmetrics.getAscent();
        m.descent = (int) lmetrics.getDescent();
        m.textheight = (int) lmetrics.getHeight();
        m.fm = g2.getFontMetrics(font);
        m.advance = m.fm.getMaxAdvance();
        //loop thru usable ascii chars: 32(space) to 126()
        for (int p = 32; p < 127; p++) {
            m.charmap.put((char) p, m.fm.charWidth(p));
        }
        m.charmap.put('\n', m.fm.charWidth('m'));//not really needed anymore
        return m;
    }
    public static char[] harvest(File file){//filters '\r'
        if(!file.exists()) return null;
        try {
            FileInputStream in = new FileInputStream(file);
            char[] cd = new char[in.available()];//no clue, monitor!
            int got;
            int cursor = 0;
            while((got = in.read()) != -1){
                char ch = (char)got;
                if(ch != '\r'){
                    cd[cursor++] = ch;
                }
            }
            in.close();
            return cd;
        } catch (IOException e) {
            //modelValid = true;//abort layout
        }
        return null;
    }
    public static char[] harvest(String input){
        if(input == null || input.length() == 0) return null;
        int len = input.length();
        int cursor = 0;
        char[] cd = new char[len];
        for (int p = 0; p < len; p++) {
            char got = input.charAt(p);
            if(got != '\r') cd[cursor++] = got;
        }
        return cd;
    }
    public static boolean isLeftMouseButton(int modifiers){
        return ((modifiers & InputEvent.BUTTON1_MASK) != 0);//thats same as button1_mask
    }
    public static boolean isRightMouseButton(int modifiers){
        return ((modifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK);// uses a binary AND
    }
}
