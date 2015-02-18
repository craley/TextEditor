/**
 *
 * 
 *
 */

package realdeal;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * Purpose: 
 * @author chris
 */
public class CaretSys implements ActionListener {
    Model model;
    ScrollSys scroll;
    Metrics metrics;
    LayoutSys layout;
    
    int line;
    int column;
    
    boolean active;
    boolean caretVisible;
    int caretWidth = 4;
    int blinkRate = 500;//ms between timer firings
    Timer flasher;
    Color color = Color.black;
    
    //for caret repaint requests
    Textbox parent;//NOT MODULAR---FIXXXXX
    
    public CaretSys(Textbox tb){
        parent = tb;
    }
    public void setScroll(ScrollSys ss){
        scroll = ss;
    }
    public void setLayout(LayoutSys ls){
        layout = ls;
    }
    public void setMetrics(Metrics m){
        metrics = m;
    }
    public void setModel(Model m){
        model = m;
    }
    /**
     * Used to disable caret for non-editable config.
     * @param isEnabled 
     */
    public void setEnabled(boolean isEnabled){
        active = isEnabled;
        if(active){
            flasher = new Timer(blinkRate, this);
            flasher.start();
        } else {
            if(flasher != null){
                flasher.stop();
                flasher.removeActionListener(this);
                flasher = null;
            }
        }
    }
    public void setCaretWidth(int width){
        
    }
    public void setCaretColor(Color c){
        
    }
    /**
     * Sets the caret blink rate.
     *
     * @param rate the rate in milliseconds, 0 to stop blinking
     * @see Caret#setBlinkRate
     */
    public void setBlinkRate(int rate) {
        if (rate != 0) {
            if (flasher == null) {
                flasher = new Timer(rate, this);
            }
            flasher.setDelay(rate);
        } else {
            if (flasher != null) {
                flasher.stop();
                flasher.removeActionListener(this);
                flasher = null;
            }
        }
    }
    /**
     * Gets the caret blink rate.
     *
     * @return the delay in milliseconds.  If this is
     *  zero the caret will not blink.
     * @see Caret#getBlinkRate
     */
    public int getBlinkRate() {
        return (flasher == null) ? 0 : flasher.getDelay();
    }
    //For paint thread to draw
    public Rectangle getCaret(){
        Point p = scroll.getCamSpacePoint(line, column);
        return new Rectangle(p.x, layout.gap + p.y, caretWidth, metrics.ascent + metrics.descent);
    }
    /**
     * Place of caret by command.
     * 
     * last column for newline: width - 1
     * last column for else:    width
     * @param line
     * @param col 
     */
    public void moveCaret(int cline, int col){
        if(line < 0 || cline >= model.nlines) return;
        line = cline;
        //int maxw = isNewlineTerminated(line) ? model.getWidth(line) - 1 : model.getWidth(line);
        //if(col > maxw) col = maxw;
        int last = model.getLast(line);
        column = Math.min(last + 1, col);
        //column = col;
    }
    /**
     * Placement of caret by mouse.
     * Allows: Last + 1
     * @param px
     * @param py 
     */
    public void moveCaretByMouse(int mx, int my){
        //convert to docSpace
        int px = mx + scroll.camx;
        int py = my + scroll.camy;
        //Outside vertically
        if(py < 0 || py > scroll.docHeight){
            //ignore
        } else if(px < 0){
            //ignore
        } else {
            //clamp to line
            int ln = scroll.getLineForPoint(py);
            if(ln >= 0 && ln < model.nlines){
                int lx = scroll.hybridPointToColumn(ln, px);
                //if too far right, place at Last + 1
                if(lx == -2) lx = model.getLast(ln) + 1;
                line = ln; column = lx;
            }
        }
    }
    /**
     * Placement of caret by keyboard arrows.
     * Invariant: Caret allowed to be at Last + 1
     */
    public void caretDown(){
        if(line < model.getLines() - 1){//cant go down
            line++;
            //handle shorter width on new line
            column = Math.min(column, model.getLast(line) + 1);
        }
    }
    public void caretUp(){//control is in the wrong spot: should be outside
        if(line > 0){
            line--;
            column = Math.min(column, model.getLast(line) + 1);
        }
    }
    public void caretRight(){
        if(column < model.getLast(line) + 1){//nix -1
            column++;
        } else if(line < model.getLines() - 1){
            line++;
            column = 0;
        }
    }
    public void caretLeft(){
        if(column > 0){
            column--;
        } else if(line > 0){//col is 0
            line--;
            column = model.getLast(line) + 1;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        caretVisible = !caretVisible;
        parent.repaint();
    }
}
