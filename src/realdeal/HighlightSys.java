/**
 *
 * 
 *
 */

package realdeal;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 *
 * @author chris
 */
public class HighlightSys {
    int startLine, startColumn;
    int endLine, endColumn;
    boolean visible;
    
    Metrics metrics;
    ScrollSys scroll;
    LayoutSys layout;
    Model model;
    CaretSys caret;
    Color color = new Color(255,0,255,100);

    public HighlightSys(){
        
    }
    public void setMetrics(Metrics m){
        metrics = m;
    }
    public void setScroll(ScrollSys ss){
        scroll = ss;
    }
    public void setLayout(LayoutSys ls){
        layout = ls;
    }
    public void setCaret(CaretSys cr){
        caret = cr;
    }
    public void setModel(Model m){
        model = m;
    }
    public Shape getHighlight(){
        int minLine = 0, maxLine = 0, minCol = 0, maxCol = 0;
        int availWidth = scroll.getAvailableWidth();
        if(startLine == endLine){
            //Rectangle
            //int py = lineToPoint(endLine) + layout.gap;
            int py = scroll.getPointForLine(endLine) + layout.gap;
            int mincol = startColumn <= endColumn ? startColumn : endColumn;
            int maxcol = startColumn <= endColumn ? endColumn : startColumn;
            int ax = scroll.columnToPoint(startLine, mincol);
            int width = scroll.columnToPoint(startLine, maxcol + 1) - ax;
            return new Rectangle(ax - scroll.camx, py - scroll.camy, width, metrics.ascent + metrics.descent);
        }
        //Determine the smallest line
        if(startLine <= endLine){
            minLine = startLine;
            minCol = startColumn;
            maxLine = endLine;
            maxCol = endColumn;
        } else {
            minLine = endLine;
            minCol = endColumn;
            maxLine = startLine;
            maxCol = startColumn;
        }
        int vMin = scroll.getPointForLine(minLine) + layout.gap - scroll.camy;
        int vMax = scroll.getPointForLine(maxLine) + layout.gap - scroll.camy;
        int hMin = scroll.columnToPoint(minLine, minCol) - scroll.camx;
        int hMax = scroll.columnToPoint(maxLine, maxCol) - scroll.camx;
        int maxWidth = metrics.charmap.get(model.getChar(maxLine, maxCol));
        GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        gp.moveTo(hMin, vMin);
        gp.lineTo(availWidth, vMin);
        gp.lineTo(availWidth, vMax);
        gp.lineTo(hMax + maxWidth, vMax);
        gp.lineTo(hMax + maxWidth, vMax + metrics.ascent + metrics.descent);
        gp.lineTo(0, vMax + metrics.ascent + metrics.descent);
        gp.lineTo(0, vMin + metrics.ascent + metrics.descent);
        gp.lineTo(hMin, vMin + metrics.ascent + metrics.descent);
        gp.closePath();
        return gp;
    }
    boolean dragging;
    /**
     * Strategy: on mouse press- reject anything not on text
     *           on mouse drag - clamp to text end.
     * @param mx
     * @param my 
     */
    public void mousePress(int mx, int my){
        
        //need to reject points outside of document!(doc may not fill entire viewport)
        Point sp = scroll.getPosition(mx, my);//returns a (line, col)
        if(sp != null){
            dragging = true;
            startLine = endLine = sp.x;//endLine = 
            startColumn = endColumn = sp.y;//endColumn = 
            
        }
        
    }
    public void mouseRelease(){
        dragging = false;
    }
    public void mouseDragged(int currx, int curry){
        if(dragging){
            visible = true;
            Point endp = scroll.getPositionClamp(currx, curry);//wont return null
            endLine = endp.x;
            endColumn = endp.y;
            
            if((startLine == endLine && endColumn > startColumn) || endLine > startLine){
                //caret.column++;//interesting phenonemon to make caret outside highlight
                caret.moveCaret(endLine, endColumn + 1);
            } else {
                caret.moveCaret(endLine, endColumn);
            }
        }
    }
    /**
     * 1 click: clear any highligh
     * 2 clicks: highlight word
     * 3 clicks: highlight text portion of line
     * 4 clicks: highlight entire freakin line(including whitespace)
     * @param countCount
     * @param mx
     * @param my 
     */
    public void mouseClicked(int count, int mx, int my){
        if(count == 1){
            visible = false;
        } else if(count == 2){
            highlightAlpha(mx, my);
        } else if(count == 3){
            highlightWord(mx, my);
        } else if(count == 4){
            highlightTextLine(mx, my);
        } else if(count == 5){
            highlightEntireLine(mx, my);
        }
    }
    private void highlightAlpha(int mx, int my){
        Point sp = scroll.getPosition(mx, my);
        if(sp == null) return;
        Range range = model.alphaNumericBounds(sp.x, sp.y);
        if(range == null) return;
        visible = true;
        startLine = sp.x;
        startColumn = range.start;
        endLine = sp.x;
        endColumn = range.end;
    }
    private void highlightWord(int mx, int my){
        Point sp = scroll.getPosition(mx, my);
        if(sp == null) return;
        Range range = model.wordBounds(sp.x, sp.y);
        if(range == null) return;
        visible = true;
        startLine = sp.x;
        startColumn = range.start;
        endLine = sp.x;
        endColumn = range.end;
        //Move caret to end of selection
        //caret.line = sp.x;//should already be there, but just for sure
        //caret.col = range.end + 1;//watch this, may need to verify this always ok
    }
    private void highlightTextLine(int mx, int my){
        Point sp = scroll.getPosition(mx, my);
        if(sp == null) return;
        Range range = model.printableBounds(sp.x);
        if(range == null) return;
        visible = true;
        startLine = sp.x;
        startColumn = range.start;
        endLine = sp.x;
        endColumn = range.end;
        //Move caret to end of selection
        //caretPos.line = sp.x;//should already be there, but just for sure
        //caretPos.col = range.end + 1;//watch this, may need to verify this always ok
    }
    private void highlightEntireLine(int mx, int my){
        visible = false;
    }
    
    //Editor Actions
    private char[] clipboard;
    /**
     * Remove text from model, load into clipboard
     */
    public void cut(){
        if(visible){
            clipboard = model.get(startLine, startColumn, endLine, endColumn);
            model.delete(startLine, startColumn, endLine, endColumn);
            visible = false;
        }
    }
    public void copy(){
        if(visible){
            clipboard = model.get(startLine, startColumn, endLine, endColumn);
            visible = false;
        }
    }
    public void paste(){
        if(clipboard != null && clipboard.length > 0){
            //dump clipboard into caret position
            model.insert(caret.line, caret.column, clipboard);
        }
    }
    public void selectAll(){
        
    }
}
