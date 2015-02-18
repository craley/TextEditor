 /**
 *
 * 
 *
 */

package realdeal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 *
 * @author chris
 */
public class ScrollSys {
    int camx, camy;
    //protect these
    int camWidth, camHeight;
    int docWidth, docHeight;//set using method!
    
    int scrollSpeed = 16;
    int blockSpeed = 34;
    float zoom = 1.0f;
    
    //horiz slider
    int hSliderHeight = 15;
    int hbarX;
    int hbarWidth;
    //vert slider
    int vSliderWidth = 15;
    int vbarY;
    int vbarHeight;
    //both
    private static final int MIN_BAR = 30;
    
    int trackInsets = 1;
    int bothInsets = trackInsets + trackInsets;
    
    Model model;
    Metrics metrics;
    LayoutSys layout;
    
    public static final int ALWAYS = 0;
    public static final int WHEN_NEEDED = 1;
    public static final int NEVER = 2;
    
    int horizScrollPolicy = WHEN_NEEDED;
    int vertScrollPolicy = WHEN_NEEDED;
    
    boolean horizExists;
    boolean vertExists;
    
    int lastWidth, lastHeight;
    int availWidth, availHeight;
    
    /**
     * Acts as a filter for mouse events.
     * With every key stroke, the cell is tested for visibility.
     * Scrollbar activity is a function of model size and scroll policy.
     */

    public ScrollSys(){
        
    }
    public void setLayout(LayoutSys ls){
        layout = ls;
    }
    public void setModel(Model m){
        model = m;
    }
    public void setMetrics(Metrics m){
        metrics = m;
    }
    public int getAvailableWidth(){
        return availWidth;
    }
    public int getAvailableHeight(){
        return availHeight;
    }
    public void setHorizScrollPolicy(int setting){
        horizScrollPolicy = setting;
        revalidate();
    }
    public void setVerticalScrollPolicy(int setting){
        vertScrollPolicy = setting;
        revalidate();
    }
    /**
     * Version 1
     * Change: When either the model size changes or the
     * window size changes: the scroller must update.
     */
    private void oldRevalidate(){
        //Resize the horiz slider handle
        if(horizScrollPolicy != ALWAYS && docWidth < camWidth){
            horizExists = false;
        }
        if(horizExists && lastWidth > 0){//Already out   Maybe: Lastwidth != CurrentWidth
            int denom = lastWidth - hbarWidth;//old bar width
            if(denom == 0){//should be just turning off unless ALWAYS
                hbarX = 0;
                hbarWidth = camWidth;
            } else {
                hbarWidth = Math.max(30, (camWidth * camWidth) / docWidth);//impose lower limit on handle size
                hbarX = hbarX * (camWidth - hbarWidth) / denom;
            }
            //camx is unaffected as ratio is maintained
        }
        //See if horiz slider needs to be activated
        if(!horizExists && horizScrollPolicy != NEVER && docWidth >= camWidth){//if not present, allowed to be present, and needs to be present..
            horizExists = true;
            hbarX = 0;
            hbarWidth = Math.max(30, (camWidth * camWidth) / docWidth);//impose lower limit on handle size
            
        }
        if(vertScrollPolicy != ALWAYS && docHeight < camHeight){
            vertExists = false;
        }
        //Resize the vertical slider handle
        if(vertExists && lastHeight > 0){
            int denom = lastHeight - vbarHeight;
            if(denom == 0){
                vbarY = 0;
                vbarHeight = camHeight;
            } else {
                //update height of slider
                vbarHeight = Math.max(30, (camHeight * camHeight) / docHeight);
                //update location of slider
                vbarY = vbarY * (camHeight - vbarHeight) / denom;
            }
            //camy is unaffected as ratio is maintained.
        }
        if(!vertExists && vertScrollPolicy != NEVER && docHeight >= camHeight){
            vertExists = true;
            vbarY = 0;
            vbarHeight = Math.max(30, (camHeight * camHeight) / docHeight);
        }
    }
    /**
     * Called from ComponentListener for window resizes.
     * Note: Handler will fire this method even before the
     * first drawing of the panel.
     * @param ncamw
     * @param ncamh 
     */
    public void resize(int ncamw, int ncamh){//version 4
        if(camWidth == 0 || camHeight == 0){
            camWidth = lastWidth = ncamw;
            camHeight = lastHeight = ncamh;
        }
        //update cam size
        camWidth = ncamw; camHeight = ncamh;
        core();
        //INITIATE MODEL REVALIDATION FOR WORD WRAP.................
        
        
        //Set for next time
        lastWidth = camWidth;
        lastHeight = camHeight;
    }
    public void validateFromModel(int dwidth, int dheight){
        docWidth = dwidth; docHeight = dheight;
        if(camWidth > 0) core();
    }
    /**
     * Special case: when last time the was a scroll distance of zero,
     * a divide by zero will occur in:
     * camx = camx * (docWidth - camWidth) / (docWidth - lastWidth);
     * 
     * currScrollDist = docWidth - camWidth
     * prevScrollDist = docWidth - lastWidth
     */
    private void core(){
        if(horizScrollPolicy == ALWAYS){
            horizExists = true;
            if(docWidth <= camWidth){//Protects against docWidth - camWidth == 0 (currently, no scroll distance)
                hbarX = trackInsets;
                hbarWidth = camWidth - bothInsets;
                camx = 0;
            } else {
                int trackWidth = camWidth - bothInsets;
                int prevScrollDist = docWidth - lastWidth;
                //Use ratio to determine new cam position
                //
                if(prevScrollDist > 0) camx = camx * (docWidth - camWidth) / prevScrollDist;//protects against no scroll distance last time.
                hbarWidth = (trackWidth * camWidth) / docWidth;
                if(hbarWidth < MIN_BAR) hbarWidth = MIN_BAR;
                hbarX = camx * (trackWidth - hbarWidth) / (docWidth - camWidth);
            }
        } else if(horizScrollPolicy == WHEN_NEEDED){
            if(docWidth <= camWidth){
                horizExists = false;
                hbarX = trackInsets;
                hbarWidth = camWidth - bothInsets;
            } else {
                horizExists = true;
                int trackWidth = camWidth - bothInsets;
                if(vertExists) trackWidth -= (vSliderWidth + bothInsets);
                int prevScrollDist = docWidth - lastWidth;
                //Use ratio to determine new cam position
                if(prevScrollDist > 0) camx = camx * (docWidth - camWidth) / prevScrollDist;// ratio: currentScrollDist / previousScrollDist
                hbarWidth = (trackWidth * camWidth) / docWidth;
                if(hbarWidth < MIN_BAR) hbarWidth = MIN_BAR;
                hbarX = camx * (trackWidth - hbarWidth) / (docWidth - camWidth);
            }
        } else {
            //Never
            horizExists = false;
        }
        
        if(vertScrollPolicy == ALWAYS){
            vertExists = true;
            if(docHeight <= camHeight){
                vbarY = trackInsets;
                vbarHeight = camHeight - bothInsets;
                camy = 0;
            } else {
                int prevScrollDist = docHeight - lastHeight;
                int trackHeight = camHeight - bothInsets;
                if(horizExists) trackHeight -= (hSliderHeight + bothInsets);
                if(prevScrollDist > 0) camy = camy * (docHeight - camHeight) / prevScrollDist;
                vbarHeight = (trackHeight * camHeight) / docHeight;
                if(vbarHeight < MIN_BAR) vbarHeight = MIN_BAR;
                vbarY = camy * (trackHeight - vbarHeight) / (docHeight - camHeight);
            }
        } else if(vertScrollPolicy == WHEN_NEEDED){
            if(docHeight <= camHeight){
                vertExists = false;
                vbarY = trackInsets;
                vbarHeight = camHeight - bothInsets;
                camy = 0;
            } else {
                vertExists = true;
                int prevScrollDist = docHeight - lastHeight;
                int trackHeight = camHeight - bothInsets;
                if(horizExists) trackHeight -= (hSliderHeight + bothInsets);
                if(prevScrollDist > 0) camy = camy * (docHeight - camHeight) / prevScrollDist;
                vbarHeight = (trackHeight * camHeight) / docHeight;
                if(vbarHeight < MIN_BAR) vbarHeight = MIN_BAR;
                vbarY = camy * (trackHeight - vbarHeight) / (docHeight - camHeight);
            }
        } else {
            //Never
            vertExists = false;
        }
        //Establish availables
        if(horizExists){
            availHeight = camHeight - hSliderHeight - bothInsets;
        } else {
            availHeight = camHeight;
        }
        if(vertExists){
            availWidth = camWidth - vSliderWidth - bothInsets;
        } else {
            availWidth = camWidth;
        }
    }
    public void test(){
        System.out.println("\ncamx: " + camx + ", camy: " + camy);
        System.out.println("docwidth: " + docWidth + ", docHeight: " + docHeight);
        System.out.println("camWidth: " + camWidth + ", camHeight: " + camHeight);
        System.out.println("Horiz barX: " + hbarX + ", width: " + hbarWidth);
        System.out.println("Vert barY: " + vbarY + ", height: " + vbarHeight);
    }
    /**
     * Model origin is anchor.
     * ScrDist = docWidth - camWidth
     * camx can range from 0 to ScrDist
     */
    public void revalidate(){//version 2: Not needed anymore
        if(camWidth == 0 || camHeight == 0) return;//dont have dependencies yet
        int temp = 0;
        //handle horiz bar
        if(horizScrollPolicy == ALWAYS){
            horizExists = true;
            if(docWidth <= camWidth){
                hbarX = trackInsets;
                hbarWidth = camWidth - bothInsets;
                camx = 0;
            } else {
                int lastBarWidth = hbarWidth;
                temp = (camWidth * camWidth) / docWidth;//model can have 0 width!!
                hbarWidth = temp < MIN_BAR ? MIN_BAR : temp;
                if(lastWidth == 0 || hbarX <= trackInsets){
                    hbarX = trackInsets;//first time
                    camx = 0;
                } else {//use ratio when: not first time, 
                    hbarX = hbarX * (camWidth - hbarWidth) / (lastWidth - lastBarWidth);
                    camx = hbarX * (docWidth - camWidth) / (camWidth - hbarWidth);//non-linear formula
                }
            }
        } else if(horizScrollPolicy == WHEN_NEEDED){
            //TRYING THIS NEW SUB-SECTION BELOW. ORIGINAL COMMENTED OUT BELOW IT.
            if(docWidth <= camWidth){
                horizExists = false;
                hbarX = trackInsets;
                hbarWidth = camWidth - bothInsets;
                camx = 0;
            } else {
                horizExists = true;
                int lastBarWidth = hbarWidth;
                temp = (camWidth * camWidth) / docWidth;
                hbarWidth = temp < MIN_BAR ? MIN_BAR : temp;
                if(lastWidth == 0 || hbarX <= trackInsets){
                    hbarX = trackInsets;//first time
                    camx = 0;
                } else {//cant use ratio when: no last time, last time bar took up whole screen
                    hbarX = hbarX * (camWidth - hbarWidth) / (lastWidth - lastBarWidth);
                    camx = hbarX * (docWidth - camWidth) / (camWidth - hbarWidth);//non-linear formula
                }
            }
//            if(horizExists){
//                if(docWidth <= camWidth){//Model smaller than cam
//                    horizExists = false;
//                    hbarX = trackInsets;
//                    hbarWidth = camWidth - bothInsets;
//                } else {//Model larger than cam
//                    int lastBarWidth = hbarWidth;
//                    temp = (camWidth * camWidth) / docWidth;
//                    hbarWidth = temp < MIN_BAR ? MIN_BAR : temp;
//                    if(lastWidth == 0 || hbarX <= trackInsets){
//                        hbarX = trackInsets;//first time
//                        camx = 0;
//                    } else {//cant use ration when: no last time, last time bar took up whole screen
//                        hbarX = hbarX * (camWidth - hbarWidth) / (lastWidth - lastBarWidth);
//                        camx = hbarX * (docWidth - camWidth) / (camWidth - hbarWidth);//non-linear formula
//                    }
//                }
//            } else {//Bar not out
//                if(docWidth <= camWidth){
//                    //no-op
//                } else {//cam shrunk or model enlarged
//                    horizExists = true;
//                    temp = (camWidth * camWidth) / docWidth;
//                    hbarWidth = temp < MIN_BAR ? MIN_BAR : temp;
//                    hbarX = trackInsets;
//                    camx = 0;
//                }
//            }
        } else {//Never
            horizExists = false;
        }
        //handle vertical bar
        if(vertScrollPolicy == ALWAYS){
            vertExists = true;
            if(docHeight <= camHeight){
                vbarY = trackInsets;
                vbarHeight = camHeight - bothInsets;
                camy = 0;
            } else {
                //2 cases: size diff and not diff
                int lastBarHeight = vbarHeight;
                temp = (camHeight * camHeight) / docHeight;
                vbarHeight = temp < MIN_BAR ? MIN_BAR : temp;
                if(lastHeight == 0 || vbarY <= trackInsets){
                    vbarY = trackInsets;//first time
                    camy = 0;
                } else {//use ratio when: not first time, 
                    vbarY = vbarY * (camHeight - vbarHeight) / (lastHeight - lastBarHeight);
                    camy = vbarY * (docHeight - camHeight) / (camHeight - vbarHeight);//crazy
                }
            }
        } else if(vertScrollPolicy == WHEN_NEEDED){
            if(docHeight <= camHeight){
                vertExists = false;
                vbarY = trackInsets;
                vbarHeight = camHeight - bothInsets;
                camy = 0;
            } else {
                vertExists = true;
                int lastBarHeight = vbarHeight;
                temp = (camHeight * camHeight) / docHeight;
                vbarHeight = temp < MIN_BAR ? MIN_BAR : temp;
                if(lastHeight == 0 || vbarY <= trackInsets){
                    vbarY = trackInsets;//first time
                    camy = 0;
                } else {//cant use ration when: no last time, last time bar took up whole screen
                    vbarY = vbarY * (camHeight - vbarHeight) / (lastHeight - lastBarHeight);
                    camy = vbarY * (docHeight - camHeight) / (camHeight - vbarHeight);//crazy
                }
            }
//            if(vertExists){
//                if(docHeight <= camHeight){//Model smaller or empty
//                    vertExists = false;
//                    vbarY = trackInsets;
//                    vbarHeight = camHeight - bothInsets;
//                } else {//Model larger than cam
//                    int lastBarHeight = vbarHeight;
//                    temp = (camHeight * camHeight) / docHeight;
//                    vbarHeight = temp < MIN_BAR ? MIN_BAR : temp;
//                    if(lastHeight == 0 || vbarY <= trackInsets){
//                        vbarY = trackInsets;//first time
//                        camy = 0;
//                    } else {//cant use ration when: no last time, last time bar took up whole screen
//                        vbarY = vbarY * (camHeight - vbarHeight) / (lastHeight - lastBarHeight);
//                        camy = vbarY * (docHeight - camHeight) / (camHeight - vbarHeight);//crazy
//                    }
//                }
//            } else {//Bar not out
//                if(docHeight <= camHeight){
//                    //no-op
//                } else {//cam shrunk or model enlarged
//                    vertExists = true;
//                    temp = (camHeight * camHeight) / docHeight;
//                    vbarHeight = temp < MIN_BAR ? MIN_BAR : temp;
//                    vbarY = trackInsets;
//                }
//            }
        } else {//Never
            vertExists = false;
        }
    }
    /**
     * Instead of defining a line to position in upper right,
     * have 3 cases:
     *   1. View is showing line: do nothing.
     *   2. Line is above view: scroll top till line visible.
     *   3. Line is below view: scroll bottom till line visible.
     * 
     * To ensure visibility, the top of the line must be greater than 0
     * and the bottom of the line must be less than available height. (in cam coords)
     * 
     * @param line
     * @param col 
     */
    public void makeVisible(int line, int col){
        int cyTop = line * (metrics.textheight + layout.gap) - camy;
        int cyBottom = cyTop + (metrics.textheight + layout.gap);
        
        if(cyTop < 0){
            //Scroll up(remem, camy is expressed in model coords!)
            camy = camy + cyTop;
            if(vertExists) vbarY = camy * (availHeight - vbarHeight) / (docHeight - camHeight);
        } else if(cyBottom >= availHeight){//
            camy = camy + (cyBottom - availHeight);
            if(vertExists) vbarY = camy * (availHeight - vbarHeight) / (docHeight - camHeight);
        }
        //Now handle horiz
        int cxLeft = columnToPoint(line, col) - camx;
        int cxRight = cxLeft + 10;
        //use cam coords to determine visibility
        if(cxLeft < 0){
            camx = camx + cxLeft;//math must both be in model coords
            if(horizExists) hbarX = camx * (availWidth - hbarWidth) / (docWidth - camWidth);
        } else if(cxRight >= availWidth){
            camx = camx + (cxRight - availWidth);
            if(horizExists) hbarX = camx * (availWidth - hbarWidth) / (docWidth - camWidth);
        }
    }
    public boolean isCellVisible(int line, int col){
        int cyTop = line * (metrics.textheight + layout.gap) - camy;
        int cyBottom = cyTop + (metrics.textheight + layout.gap);
        if(cyTop >= 0 && cyBottom < getAvailableHeight()){//
            int cxLeft = columnToPoint(line, col) - camx;
            int cxRight = cxLeft + 10;//made up for now
            if(cxLeft >= 0 && cxRight < getAvailableWidth()){
                return true;
            }
        }
        return false;
    }
    
    public void drawScroller(Graphics2D g){
        //int availWidth = vertExists ? camWidth - vSliderWidth - bothInsets: camWidth;
        //int availHeight = horizExists ? camHeight - hSliderHeight - bothInsets: camHeight;
        if(horizExists){
            int vRail = camHeight - hSliderHeight - bothInsets;
            g.setColor(Color.black);
            g.drawRect(0, vRail, availWidth, hSliderHeight + bothInsets);//draw track rectangle
            g.setColor(Color.lightGray);
            g.fillRect(0, vRail, availWidth, hSliderHeight + bothInsets);//fill track rectangle
            g.setColor(Color.darkGray);
            g.drawRect(hbarX, vRail + trackInsets, hbarWidth, hSliderHeight);//draw handle
            g.fillRect(hbarX, vRail + trackInsets, hbarWidth, hSliderHeight);//fill handle
        }
        if(vertExists){
            int hRail = camWidth - vSliderWidth - bothInsets;
            g.setColor(Color.black);
            g.drawRect(hRail, 0, vSliderWidth + bothInsets, availHeight);
            g.setColor(Color.lightGray);
            g.fillRect(hRail, 0, vSliderWidth + bothInsets, availHeight);
            g.setColor(Color.darkGray);
            g.drawRect(hRail + trackInsets, vbarY, vSliderWidth, vbarHeight);
            g.fillRect(hRail + trackInsets, vbarY, vSliderWidth, vbarHeight);
        }
        
    }
    boolean horizDrag;
    boolean vertDrag;
    int startX, startY;
    //Signals true that someone else needs to handle
    public boolean mousePressed(int mx, int my){
        //if horiz scroll present and press is on the handle
        if(horizExists && my >= camHeight - hSliderHeight && mx >= hbarX && mx <= hbarX + hbarWidth){
            //handle here
            horizDrag = true;
            startX = mx;
            startY = my;
            return false;
        } else if(vertExists && mx >= camWidth - vSliderWidth && my >= vbarY && my <= vbarY + vbarHeight){
            vertDrag = true;
            startX = mx;
            startY = my;
            return false;
        }
        return true;
    }
    public boolean mouseReleased(){
        horizDrag = false;
        vertDrag = false;
        return true;
    }
    /**
     * Ratio:  (camx / barx) = (docWidth - camWidth) / (availWidth - barWidth)
     * @param curx
     * @param cury
     * @return 
     */
    public boolean mouseDragged(int curx, int cury){
        if(horizDrag){
            int move = curx - startX;
            if(move > 0 && hbarX < availWidth - hbarWidth - move){
                hbarX = hbarX + move;
                camx = hbarX * (docWidth - camWidth) / (availWidth - hbarWidth);//non-linear formula
            } else if(move > 0 && hbarX < availWidth - hbarWidth){
                hbarX = availWidth - hbarWidth;//max barX
                camx = docWidth - camWidth;//max camx
            } else if(move < 0 && hbarX > -move){
                hbarX = hbarX + move;
                camx = hbarX * (docWidth - camWidth) / (availWidth - hbarWidth);//non-linear formula
            } else if(move < 0 && hbarX > 0){
                hbarX = trackInsets;
                camx = 0;
            }
            startX = curx;
            return false;
        } else if(vertDrag){
            int move = cury - startY;
            if(move > 0 && vbarY < availHeight - vbarHeight - move){
                vbarY = vbarY + move;
                camy = vbarY * (docHeight - camHeight) / (availHeight - vbarHeight);
            } else if(move > 0 && vbarY < availHeight - vbarHeight){
                vbarY = availHeight - vbarHeight;
                camy = docHeight - camHeight;
            } else if(move < 0 && vbarY > -move){
                vbarY = vbarY + move;
                camy = vbarY * (docHeight - camHeight) / (availHeight - vbarHeight);
            } else if(move < 0 && vbarY > 0){
                vbarY = trackInsets;
                camy = 0;
            }
            startY = cury;
            return false;
        }
        return true;
    }
    //Move by block units
    public boolean mouseClicked(int mx, int my){
        if(horizExists && my >= camHeight - hSliderHeight){//and not on handle(if on handle, press and drag takes care of it)
            if(mx < hbarX){
                hbarX = Math.max(hbarX - blockSpeed, 0);
                camx = hbarX * (docWidth - camWidth) / (availWidth - hbarWidth);//non-linear formula
            } else if(mx > hbarX + hbarWidth){
                hbarX = Math.min(hbarX + blockSpeed, availWidth - hbarWidth);
                camx = hbarX * (docWidth - camWidth) / (availWidth - hbarWidth);//non-linear formula
            }
            return false;
        } else if(vertExists && mx >= camWidth - vSliderWidth){
            if(my < vbarY){
                vbarY = Math.max(vbarY - blockSpeed, 0);
                camy = vbarY * (docHeight - camHeight) / (availHeight - vbarHeight);
            } else if(my > vbarY + vbarHeight){
                vbarY = Math.min(vbarY + blockSpeed, camHeight - vbarHeight);
                camy = vbarY * (docHeight - camHeight) / (availHeight - vbarHeight);
            }
            return false;
        }
        return true;
    }
    public void mouseWheel(int ticks){
        if(ticks == -1){
            scrollUp();
        } else if(ticks == 1){
            scrollDown();
        }
    }
    //Now, scrolling mouse must change scrollbars if present
    public void scrollUp(){//Decrease Y
        if(camy >= scrollSpeed){
            camy -= scrollSpeed;
            if(vertExists) vbarY = camy * (availHeight - vbarHeight) / (docHeight - camHeight);
            //updateCamera();
        } else if(camy > 0){
            camy = 0;
            vbarY = trackInsets;
        }
    }
    public void scrollDown(){//Increase Y
        if(camy + camHeight < docHeight - scrollSpeed){
            camy += scrollSpeed;
            if(vertExists) vbarY = camy * (availHeight - vbarHeight) / (docHeight - camHeight);
        } else if(camy + camHeight < docHeight){
            camy = docHeight - camHeight;
            if(vertExists) vbarY = availHeight - vbarHeight;
        }
    }
    //Programmatic control also move scrollbars if present.
    public void scrollRight(){
        if(camx + camWidth < docWidth - scrollSpeed){
            camx += scrollSpeed;
            if(horizExists) hbarX = camx * (availWidth - hbarWidth) / (docWidth - camWidth);
        } else if(camx + camWidth < docWidth){
            camx = docWidth - camWidth;
            if(horizExists) hbarX = availWidth - hbarWidth;
        }
    }
    public void scrollLeft(){
        if(camx >= scrollSpeed){
            camx -= scrollSpeed;
            if(horizExists) hbarX = camx * (availWidth - hbarWidth) / (docWidth - camWidth);
        } else if(camx > 0){
            camx = 0;
            hbarX = trackInsets;
        }
    }
    /**
     * Converts a horiz doc space point to a column
     * Accepts point in docSpace.
     * Can only measure to last.
     * Anything to right of last just returns last.
     * @param px
     * @return 
     */
    public int pointToColumn(int line, int px){//Last: the last usable char on a line(last points to char before newline)
        model.setArrow(line);
        int last = model.arrowLast();
        if(last < 0) return 0;//nothing to measure
        int offset = 0;
        for (int col = 0; col <= last; col++) {
            offset += metrics.charmap.get(model.charAtArrow(col));
            if(offset > px) return col;
        }
        return last;
    }
    //Ret: -1 too far left, -2 if past last
    public int hybridPointToColumn(int line, int px){
        model.setArrow(line);
        int last = model.arrowLast();
        if(last < 0 || px < 0) return -1;//nothing to measure
        int offset = 0;
        for (int col = 0; col <= last; col++) {
            offset += metrics.charmap.get(model.charAtArrow(col));
            if(offset > px) return col;
        }
        return -2;//indicate the px is farther out than last
    }
    //No interpretation: can only measure upto, and including, the Last char
    //The diff is that if col exceeds, it just returns the largest offset
    //Can handle a line last of -1
    public int columnToPoint(int line, int col){//clamp version
        if(model.nlines == 0) return 0;//for displaying caret on empty model
        model.setArrow(line);
        int maxw = model.arrowLast() + 1;
        int use = Math.min(col, maxw);
        //if(col > maxw) col = maxw;//-1 clamp it to last column
        int offset = 0;
        for (int c = 0; c < use; c++) {
            offset += metrics.charmap.get(model.charAtArrow(c));
        }
        return offset;
    }
    public int getLineForPoint(int py){
        return py / (metrics.textheight + layout.gap);
    }
    public int getPointForLine(int line){
        return line * (metrics.textheight + layout.gap);
    }
    public Point getCamSpacePoint(int line, int col){
        //if(line < 0 || line >= model.nlines || col < 0) return null;
        Point pt = new Point();
        pt.y = line * (metrics.textheight + layout.gap) - camy;
        pt.x = columnToPoint(line, col) - camx;
        return pt;
    }
    public Point getDocSpacePoint(int line, int col){
        Point pt = new Point();
        pt.y = line * (metrics.textheight + layout.gap);
        pt.x = columnToPoint(line, col);
        return pt;
    }
    //For highlighting
    //Non-clamping version. Returns null if outside lines
    public Point getPosition(int cx, int cy){
        int dx = cx + camx;
        int dy = cy + camy;
        int line = dy / (metrics.textheight + layout.gap);
        if(line < 0 || line >= model.getLines()) return null;
        int col = hybridPointToColumn(line, dx);//change this for netbeans style highlighting: clamp column only
        //if(col < 0) return null;//catches too far left, too far right
        if(col == -1) col = 0;
        if(col == -2) col = model.getLast(line);
        return new Point(line, col);
    }
    public Point getPositionClamp(int cx, int cy){
        int dx = cx + camx;
        int dy = cy + camy;
        int line = dy / (metrics.textheight + layout.gap);
        if(line < 0) line = 0;
        if(line >= model.nlines) line = model.nlines - 1;
        int col = hybridPointToColumn(line, dx);
        if(col == -1) col = 0;
        if(col == -2) col = model.getLast(line);
        return new Point(line, col);
    }
}
