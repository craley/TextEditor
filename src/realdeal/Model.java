/**
 *
 * 
 *
 */

package realdeal;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 *
 * @author chris
 */
public class Model {
    public static final char NEW_LINE = '\n';
    public int columnDefaultWidth;
    public int colorDefault;

    public int nlines;

    Line head;
    Line last;
    Line arrow;
    
    /**
     * Width: the current number of chars on a line.
     * 
     */

    public Model() {
        this(30);
    }

    //Always at least 1 line
    public Model(int estimateWidth) {
        columnDefaultWidth = estimateWidth;
        nlines = 0;
    }

    private void init(int rows) {//at least one
        head = arrow = last = new Line(columnDefaultWidth);
        nlines = 1;
        for (int x = 1; x < rows; x++) {
            Line end = new Line(columnDefaultWidth);
            end.prev = last;
            last.next = end;
            last = end;
            nlines++;
        }
    }
    public void setDefaultColor(int color) {//update existing?
        colorDefault = color;
    }

    public int getMaxColumns() {
        //no good way so just calc when needed
        int temp = 0;
        for (Line c = head; c != null; c = c.next) {
            if (c.width > temp) {
                temp = c.width;
            }
        }
        return temp;
    }

    public void clear() {
        head = last = arrow = null;
        nlines = 0;
    }

    //Arrow methods
    public void setArrow(int line) {
        arrow = nodeForLine(line, false);
    }
    public void next() {
        arrow = arrow.next;
    }
    public boolean hasNext() {//this is used to go both ways.
        return arrow != null;
    }
    public void previous() {
        arrow = arrow.prev;
    }
    public void moveToFront() {
        arrow = head;
    }
    public void moveToLast() {
        arrow = last;
    }
    public char charAtArrow(int col) {
        return arrow.data[col];
    }
    public int arrowColor(int col) {
        return arrow.cdata[col];
    }
    public int arrowWidth() {
        return arrow.width;
    }
    public int arrowCapacity(){
        return arrow.data.length;
    }
    public char[] getAllCharsAtArrow() {
        return arrow.data;
    }
    public int getColorChangeAtArrow(int start) {
        int cm = arrow.cdata[start];
        int diff = start + 1;

        while (diff < arrow.width && cm == arrow.cdata[diff]) {
            diff++;
        }
        return diff;//returns the index of end or different color
    }
    public int arrowLast(){
        if(arrow == null || arrow.width == 0) return -1;
        if(arrow.data[arrow.width - 1] == NEW_LINE) return arrow.width - 2;
        return arrow.width - 1;
    }

    public String charsAtArrow(int start, int end) {//exclusive end
        if(arrow == null) return null;
        return String.valueOf(arrow.data, start, end - start);//add 1 to make inclusive
    }
    //End Arrow Methods

    public char getChar(int line, int col) {
        Line nline = nodeForLine(line, false);//dont make more lines if
        if (nline == null) {
            return 0;
        }
        return nline.data[col];
    }

    public char[] getChars(int line, int start, int end) {
        Line nline = nodeForLine(line, false);
        if (nline == null) {
            return null;
        }
        char[] out = new char[end - start];
        System.arraycopy(nline.data, start, out, 0, end - start);
        return out;
    }

    public int getColor(int line, int col) {
        Line nline = nodeForLine(line, false);
        return nline == null ? -1 : nline.cdata[col];
    }

    public int getWidth(int line) {
        Line nline = nodeForLine(line, false);
        return nline == null ? -1 : nline.width;
    }

    public int getLines() {
        return nlines;
    }
    /**
     * Determines the total number of characters
     * present in the model. Used for creating
     * an array to export the data.
     * @return 
     */
    public int getTotalCount() {
        int sum = 0;
        for (Line p = head; p != null; p = p.next) {
            sum += p.width;
        }
        return sum;
    }

    //-1 means no symbols on line
    public int getNonPrintStart(int line) {
        Line nline = nodeForLine(line, false);//false needs to check for null
        if (nline == null || nline.width == 0) {
            return -1;
        }
        int se = 0;
        while (se < nline.width && !isSymbol(nline.data[se])) {
            se++;
        }
        if (se == nline.width) {
            return -1;//found a line of non-printables.
        }
        return se;
    }

    public char[] getLinearModel() {
        int all = getTotalCount();
        char[] out = new char[all];
        int cursor = 0;
        for (Line p = head; p != null; p = p.next) {
            for (int col = 0; col < p.width; col++) {
                out[cursor++] = p.data[col];
            }
        }
        return out;
    }
    public Range pureAlphaBounds(int line, int col){
        return getBounds(line, col, Model::isAlpha);
    }
     /**
     * Slightly misnamed. Includes all non-whitespace chars.
     * Algo: 1. Check cell of click 2. if no char, check cell to right(if any)
     * 3. if no char, check cell to left(if any) 4. if still nothing, fail to
     * find word
     *
     * @param line
     * @param col
     * @return Range, composed of inclusive bounds
     */
    public Range wordBounds(int line, int col) {
        return getBounds(line, col, Model::isSymbol);
    }
    /**
     * Tries to find nearest name(letters and numbers)
     * Designed to exclude punctuation. An additional click yields punctuation.
     * @param line
     * @param col
     * @return 
     */
    public Range alphaNumericBounds(int line, int col) {
        return getBounds(line, col, Model::isAlphaNumeric);
        //return getBounds(line, col, ch -> ch != ' ' && ch != '\n');
    }
    /**
     * Lenient Algorithm
     * Checks (line,col) for qualification.
     * If it fails, checks the column to the right.
     * If that fails, checks the column to the left.
     * @param line
     * @param col
     * @param pred
     * @return Null on failure.
     */
    public Range getBounds(int line, int col, Predicate<Character> pred){
        Line nline = nodeForLine(line, false);
        if (nline == null) return null;
        int se = 0, end = 0;//both inclusive
        if (pred.test(nline.data[col])) {//Char under cell
            se = col;
            while (se - 1 >= 0 && pred.test(nline.data[se - 1])) {
                se--;
            }
            end = col;
            while (end + 1 < nline.width && pred.test(nline.data[end + 1])) {
                end++;
            }
            return new Range(se, end);
        } else if (col < nline.width - 1 && pred.test(nline.data[col + 1])) {//Char to right
            //known: char at col empty and char at col + 1 is start of word
            se = col + 1;
            end = col + 1;
            while (end + 1 < nline.width && pred.test(nline.data[end + 1])) {
                end++;
            }
            return new Range(se, end);
        } else if (col > 0 && pred.test(nline.data[col - 1])) {//Char to left
            end = col - 1;
            se = col - 1;
            while (se - 1 >= 0 && pred.test(nline.data[se - 1])) {
                se--;
            }
            return new Range(se, end);
        }
        //else fail
        return null;
    }
    /**
     * Strict Algo: only checks (line,col)
     * @param line
     * @param col
     * @param pred
     * @return 
     */
    public Range getBoundsStrict(int line, int col, Predicate<Character> pred){
        Line nline = nodeForLine(line, false);
        if (nline == null) return null;
        int se = 0, end = 0;//both inclusive
        if (pred.test(nline.data[col])) {//Char under cell
            se = col;
            while (se - 1 >= 0 && pred.test(nline.data[se - 1])) {
                se--;
            }
            end = col;
            while (end + 1 < nline.width && pred.test(nline.data[end + 1])) {
                end++;
            }
            return new Range(se, end);
        }
        return null;
    }
    
    HashSet<String> wordset = new HashSet<>();
    int wordcolor = 1;
    public void setWordColor(Color c){
        wordcolor = c.getRGB();
        
    }
    
    public void colorBounds(int line, int col){
        Line nline = nodeForLine(line, false);
        if (nline == null || col >= nline.width) return;
        int se = 0, end = 0;//both inclusive
        if(!isAlpha(nline.data[col])){
            if(col - 1 >= 0 && isAlpha(nline.data[col - 1])){
                se = col - 1; end = col;
                //predictive while
                while(se - 1 >= 0 && isAlpha(nline.data[se - 1])) se--;
                boolean hit = wordset.contains(new String(nline.data, se, end));
                for (int s = se; s < end; s++) {
                    nline.cdata[s] = hit ? wordcolor : 0;
                }
            }
            if(col + 1 < nline.width && isAlpha(nline.data[col + 1])){
                se = col + 1; end = col + 2;
                //current while
                while(end < nline.width && isAlpha(nline.data[end])) end++;
                boolean hit = wordset.contains(new String(nline.data, se, end));
                for (int s = se; s < end; s++) {
                    nline.cdata[s] = hit ? wordcolor : 0;
                }
            }
        } else {
            se = col; end = col + 1;
            //inclusive start, exclusive end algo
            while(se - 1 >= 0 && isAlpha(nline.data[se - 1])) se--;
            while(end < nline.width && isAlpha(nline.data[end])) end++;
            boolean hit = wordset.contains(new String(nline.data, se, end));
            for (int s = se; s < end; s++) {
                nline.cdata[s] = hit ? wordcolor : 0;
            }
        }
    }
    private long timeAlgo(int[] testdata, IntUnaryOperator fa){
        long start = System.nanoTime();
        Arrays.stream(testdata).map(fa);
        long end = System.nanoTime();
        return end - start;
    }
    
    private static boolean isSymbol(char ch) {//isNotWhitespace
        return ch != ' ' && ch != '\n' && ch != '\r' && ch != '\t';
    }
    private static boolean isAlphaNumeric(char ch){
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }
    private static boolean isAlpha(char ch){
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }
    private static boolean isWhitespace(char ch){
        return ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r';
    }

    public Range printableBounds(int line) {
        Line nline = nodeForLine(line, false);
        if (nline == null || nline.width == 0) {
            return null;
        }
        int se = 0, end = nline.width - 1;
        while (se < nline.width && !isSymbol(nline.data[se])) {
            se++;
        }
        if (se == nline.width) {
            return null;//found a line of non-printables.
        }
        while (end > se && !isSymbol(nline.data[end])) {
            end--;
        }
        return new Range(se, end);//inclusive bounds
    }
    public void setTextColor(int line, int start, int end, Color c){
        setTextColor(line, start, end, c.getRGB());
    }
    public void clearTextColor(int line, int start, int end){
        setTextColor(line, start, end, 0);
    }
    public void setTextColor(int line, int start, int end, int color){
        Line nline = nodeForLine(line, false);
        if (nline == null || nline.width == 0) return;
        int min = start <= end ? start : end;
        int max = start <= end ? end : start;
        while(min < nline.width && min <= max){
            nline.cdata[min++] = color;
        }
    }
    public void setTextColorLast(int start, int end, int color){
        if(last == null || last.width == 0) return;
        int min = start <= end ? start : end;
        int max = start <= end ? end : start;
        while(min < last.width && min <= max){
            last.cdata[min++] = color;
        }
    }
    //Append Text
    public void appendLine(int line, String text){
        if(line > 0 && line < nlines){
            insert(line, getWidth(line), colorDefault, text.toCharArray());
        }
    }
    public void appendLine(int line, int color, String text){
        if(line > 0 && line < nlines){
            insert(line, getWidth(line), color, text.toCharArray());
        }
    }

    public void appendLine(int line, char... clist) {
        appendLine(line, colorDefault, clist);
    }

    public void appendLine(int line, int color, char... clist) {
//        Line nline = nodeForLine(line, true);
//        if (clist.length > nline.data.length - nline.width) {//handles infinite width.
//            nline.data = resize(nline.data, nline.data.length + 3 * clist.length);
//            nline.cdata = resize(nline.cdata, nline.data.length + 3 * clist.length);
//        }
//        for (int x = 0; x < clist.length; x++) {
//            nline.data[nline.width + x] = clist[x];
//            nline.cdata[nline.width + x] = color;
//        }
//        nline.width += clist.length;
        
        if(line >= 0 && line < nlines){
            insert(line, getWidth(line), color, clist);//need a more efficient way to get width
        }
    }
    public void appendDocument(int col, String text){
        insert(nlines, col, text.toCharArray());
    }
    /**
     * Tacks on a line to end of document with text starting at col.
     * Will terminate the prior line with a newline.
     * @param col
     * @param clist 
     */
    public void appendDocument(int col, char...clist){
        insert(nlines, col, clist);
    }
    /**
     * New Strategy. Handles single line and multi-line inserts. Can specify any
     * positive line and column. Now the model interprets newline chars
     */
    public void insert(int line, int pos, String text) {
        insert(line, pos, colorDefault, text);
    }

    public void insert(int line, int pos, char... text) {
        insert(line, pos, colorDefault, text);
    }

    public void insert(int line, int pos, int color, String text) {
        insert(line, pos, color, text.toCharArray());
    }

    //workhorse: interprets any newlines and creates them. All functionality moved into here.
    public void insert(int line, int pos, int color, char...text) {//not setting color yet
        if (text == null || text.length == 0 || line < 0 || pos < 0) {
            return;
        }
        Line nline = nodeForLine(line, true);//handles line greater than existing lines.
        int len = text.length;
        char[] rest = null;
        int[] crest = null;
        if (pos < nline.width) {//if any remainder on original line, snag...
            rest = new char[nline.width - pos];
            crest = new int[nline.width - pos];
            System.arraycopy(nline.data, pos, rest, 0, rest.length);
            System.arraycopy(nline.cdata, pos, crest, 0, crest.length);
            nline.width -= rest.length;
        } else if (pos > nline.width) {//handle case where position is farther out than existing line.
            if (pos > nline.data.length) {
                nline.data = resize(nline.data, nline.data.length + (pos - nline.width));
                nline.cdata = resize(nline.cdata, nline.cdata.length + (pos - nline.width));
            }
            while (nline.width < pos) {//pad upto pos, but not pos itself
                nline.data[nline.width] = ' ';
                nline.cdata[nline.width++] = color;
            }
        }
        
        int se = 0, trip = 0, check = 0;
        int linec = 1;//# of input lines
        for (int c = 0; c < len; c++) {//count the newlines
            if (text[c] == '\n' || text[c] == '\r') {
                linec++;
            }
        }
        int inspoint = 0;
        Line currentLine = nline;
        for (int cursor = 0; cursor < linec; cursor++) {
            trip = se;
            while (se < len && !isLineEnd(text[se])) {
                se++;//find point in ctext
            }
            inspoint = (cursor == 0) ? pos : 0;
            if (se != len) {
                se++;//consume newline
            }            //verify size
            check = se - trip;//amount to add
            if (check > currentLine.data.length - currentLine.width) {//free space
                currentLine.data = resize(currentLine.data, currentLine.data.length + check * 2);
                currentLine.cdata = resize(currentLine.cdata, currentLine.cdata.length + check * 2);
            }
            System.arraycopy(text, trip, currentLine.data, inspoint, check);
            //load color
            for (int i = 0; i < check; i++) {
                currentLine.cdata[inspoint + i] = color;
            }
            currentLine.width += check;
            if (cursor < linec - 1) {
                //insert line into model
                Line addend = new Line(columnDefaultWidth);
                if (currentLine == last) {//simple case: addLast
                    addend.prev = last;
                    last.next = addend;
                    last = addend;
                } else {
                    //currentLine is in the middle somewhere
                    addend.next = currentLine.next;
                    currentLine.next.prev = addend;
                    addend.prev = currentLine;
                    currentLine.next = addend;
                }
                currentLine = addend;
                nlines++;
            } else if (cursor == linec - 1 && rest != null) {
                if (rest.length > currentLine.data.length - currentLine.width) {
                    currentLine.data = resize(currentLine.data, currentLine.data.length + rest.length * 2);
                    currentLine.cdata = resize(currentLine.cdata, currentLine.cdata.length + rest.length * 2);
                }
                System.arraycopy(rest, 0, currentLine.data, currentLine.width, rest.length);
                System.arraycopy(crest, 0, currentLine.cdata, currentLine.width, crest.length);
                currentLine.width += rest.length;
            }
        }
    }
//    public void overwrite(int line, int col, String text){
//        
//    }
    
    public void delete(int line, int col){
        delete(line, col, line, col);
    }

    public void delete(int line, int startCol, int endCol) {
        delete(line, startCol, line, endCol);
    }

    /**
     * The mother load: too complicated. must simplify.
     */
    public void delete(int startLine, int startCol, int endLine, int endCol) {
        if (startLine < 0 || startCol < 0 || endLine < 0 || endCol < 0) {
            return;
        }
        int minLine = 0, minCol = 0, maxLine = 0, maxCol = 0;
        //Handle either direction
        if (startLine <= endLine) {
            minLine = startLine;
            minCol = startCol;
            maxLine = endLine;
            maxCol = endCol;
            if(startLine == endLine && startCol > endCol){//handle single line case where columns are backwards.
                minCol = endCol;
                maxCol = startCol;
            }
        } else {
            minLine = endLine;
            minCol = endCol;
            maxLine = startLine;
            maxCol = startCol;
        }
        Line current = nodeForLine(maxLine, false);
        if (current == null) {
            return;
        }
        //handle deleting the last thing in model
        if(nlines == 1 && current.width == 1 && minCol == 0){
            nlines = 0;
            head = last = arrow = null;
            return;
        }
        int cstart = 0, cend = 0;
        boolean already;
        //must proceed in reverse order!
        for (int row = maxLine; row >= minLine; row--) {
            already = false;
            //Case: first or last line of delete range
            if (row == maxLine || row == minLine) {
                //use maxCol, interpretation: from 0 to maxCol, unless only 1 line, then minCol to maxCol
                cstart = (minLine == maxLine || row == minLine) ? minCol : 0;//handles single line interp
                if (maxCol >= current.width) {
                    maxCol = current.width - 1;//clamp maxCol
                }
                cend = (minLine == maxLine || row == maxLine) ? maxCol : current.width - 1;

                if (current.data[cend] == '\n') {//newline can only appear at end(thats why maxCol must be clamped)
                    //if cstart = 0, then whole line gone
                    if (cstart == 0) {
                        //remove current
                        current = nixLine(current);
                        if (current == null) {//if its the last line, nixLine returns null
                            current = last;
                            already = true;//already in position, skip moving to previous
                        }
                    } else {
                        if (current.next != null) {//if another line exists
                            Line next = current.next;
                            //ensure space
                            if (next.width > current.data.length - current.width) {
                                current.data = resize(current.data, current.data.length + next.width);
                                current.cdata = resize(current.cdata, current.cdata.length + next.width);
                            }
                            System.arraycopy(next.data, 0, current.data, cstart, next.width);//move chars
                            System.arraycopy(next.cdata, 0, current.cdata, cstart, next.width);//move colors
                            current.width = next.width + cstart;
                            //remove next
                            current = nixLine(next);
                            if (current == null) {
                                current = last;
                                already = true;
                            }
                        } else {//there is no additional line, just decrease width
                            current.width = cstart;
                        }
                    }
                } else {
                    //normal delete
                    //fill in hole from right, if any
                    int size = cend - cstart + 1;
                    for (int x = cend + 1; x < current.width; x++) {//fail-fast for no moving
                        current.data[x - size] = current.data[x];
                        current.cdata[x - size] = current.cdata[x];
                    }
                    //null out empties at end, if any
                    for (int x = 0; x < size; x++) {
                        current.data[current.width - 1 - x] = 0;
                        current.cdata[current.width - 1 - x] = 0;
                    }
                    current.width -= size;
                    //catch case where line doesnt have newline: last line OR a line from wordwrap
                    if (current.width == 0) {
                        current = nixLine(current);
                        if (current == null) {
                            current = last;
                            already = true;
                        }
                    }
                }

            } else {//Middle lines: implicitly from 0 to width - 1
                //remove entire line
                current = nixLine(current);
                if (current == null) {
                    current = last;
                    already = true;
                }
            }
            //only advance if last line wasnt removed
            if (!already) {
                current = current.prev;
            }
        }
    }
    
    //removes link and returns pointer to next link, or Last if we deleted it
    private Line nixLine(Line current) {
        if (current == head) {
            head = current.next;
            current.next = null;
            head.prev = null;
            //set new position of current
            current = head;
        } else if (current == last) {
            last = current.prev;
            last.next = null;
            current.prev = null;
            //No position of current needed.
            current = null;
        } else {
            Line after = current.next;
            current.prev.next = after;
            after.prev = current.prev;
            current.next = current.prev = null;
            //set new position of current
            current = after;//line following deleted line.
        }
        nlines--;
        return current;
    }
    
    public char[] get(int line, int startCol, int endCol) {
        return get(line, startCol, line, endCol);
    }

    public char[] get(int startLine, int startCol, int endLine, int endCol) {
        if (startLine < 0 || startCol < 0 || endLine < 0 || endCol < 0) {
            return null;
        }
        int minLine = 0, minCol = 0, maxLine = 0, maxCol = 0;
        List<Character> out = new ArrayList<>();
        if (startLine <= endLine) {
            minLine = startLine;
            minCol = startCol;
            maxLine = endLine;
            maxCol = endCol;
            if(startLine == endLine && startCol > endCol){//handle single line case where columns are backwards.
                minCol = endCol;
                maxCol = startCol;
            }
        } else {
            minLine = endLine;
            minCol = endCol;
            maxLine = startLine;
            maxCol = startCol;
        }
        Line current = nodeForLine(minLine, false);
        if (current == null) {
            return null;
        }
        for (int row = minLine; row <= maxLine; row++) {
            int cstart = row == minLine ? minCol : 0;
            int cend = row == maxLine ? maxCol : current.width - 1;//invariant to newlines
            for (int col = cstart; col <= cend; col++) {
                out.add(current.data[col]);
            }
            current = current.next;
        }
        int snag = out.size();
        if (snag > 0) {
            char[] conv = new char[snag];
            for (int c = 0; c < snag; c++) {
                conv[c] = out.get(c);
            }
            return conv;
        }
        return null;
    }

    private static boolean isLineEnd(char ch) {
        return ch == '\n' || ch == '\r';
    }

    public void overwrite(int line, int pos, char... clist) {//Does not handle multi-line data
        overwrite(line, pos, colorDefault, clist);
    }

    public void overwrite(int line, int pos, int color, char... clist) {//Does not handle multi-line data
        Line nline = nodeForLine(line, false);
        if (nline == null) {
            return;
        }

        pos = pos > nline.width ? nline.width : pos;//clamp for protection below
        if (pos + clist.length > nline.data.length) {
            nline.data = resize(nline.data, nline.data.length + 3 * clist.length);
            nline.cdata = resize(nline.cdata, nline.data.length + 3 * clist.length);
        }
        //perform overwrite
        for (int x = 0; x < clist.length; x++) {
            nline.data[pos + x] = clist[x];
            nline.cdata[pos + x] = color;
        }
        if (pos + clist.length > nline.width) {
            nline.width = pos + clist.length;
        }
    }

    public void replaceLine(int line, char... clist) {//NOT FIXED. DO NOT USE
        Line nline = nodeForLine(line, true);

        if (clist != null && clist.length > 0) {//What if no data provided?
            if (clist.length > nline.data.length) {
                nline.data = resize(nline.data, clist.length + 5);
                nline.cdata = resize(nline.cdata, clist.length + 5);
            }
            System.arraycopy(clist, 0, nline.data, 0, clist.length);
            nline.width = clist.length;
        }
    }
    //Tested Solid
    public void addLine(String text){
        addLine(text.toCharArray());
    }
    //Tested Solid
    public void addLine(char[] text){
        if(text == null || text.length == 0) return;
        int len = text.length;
        int start = 0, end = 0;
        //Append newline to previous line if necessary
        if(last != null){
            if(last.width == 0){
                last.data[last.width++] = NEW_LINE;
            } else if(last.data[last.width - 1] != NEW_LINE){
                if(last.width >= last.data.length){
                    last.data = resize(last.data, last.data.length + 3);
                    last.cdata = resize(last.cdata, last.cdata.length + 3);
                }
                last.data[last.width++] = NEW_LINE;
            }
        }
        while(start < len){
            
            Line line = new Line(columnDefaultWidth);
            if(last == null){
                head = last = arrow = line;
            } else {
                line.prev = last;
                last.next = line;
                last = line;
            }
            nlines++;
            while(end < len && text[end] != NEW_LINE) end++;
            //2 options: end == len or end == newline
            if(end != len) end++;//consume newline
            int toCopy = end - start;
            if(toCopy > line.data.length){//both are "sizes"
                line.data = resize(line.data, toCopy);//new size is toCopy!
                line.cdata = resize(line.cdata, toCopy);
            }
            System.arraycopy(text, start, line.data, 0, toCopy);
            line.width = toCopy;
            start = end;
        }
    }
    //Tested Solid
    public void insertLine(int line, String text){
        insertLine(line, text.toCharArray());
    }
    //Tested Solid
    public void insertLine(int line, char[] text){
        if(text == null || text.length == 0) return;
        if(line < 0) line = 0;
        boolean over = false;
        while(nlines < line){//stops short of making requested line
            over = true;
            if(last != null){
                if(last.width == 0){
                    last.data[last.width++] = NEW_LINE;
                } else if(last.data[last.width - 1] != NEW_LINE){
                    if(last.width >= last.data.length){
                        last.data = resize(last.data, last.data.length + 3);
                        last.cdata = resize(last.cdata, last.cdata.length + 3);
                    }
                    last.data[last.width++] = NEW_LINE;
                }
            }
            Line temp = new Line(columnDefaultWidth);
            if(last == null){
                head = last = arrow = temp;
            } else {
                temp.prev = last;
                last.next = temp;
                last = temp;
            }
            nlines++;
        }
        if(over || nlines == line){//very special case: when nlines == line, no work above needed, just addLine
            addLine(text);
            return;
        }
        Line existing = nodeForLine(line, false);//empty model never comes here.
        int len = text.length;
        int start = 0, end = 0;
        while(start < len){
            Line created = new Line(columnDefaultWidth);
            if(existing == head){
                created.next = existing;
                existing.prev = created;
                head = created;
            } else {
                created.prev = existing.prev;
                created.next = existing;
                created.prev.next = created;
                existing.prev = created;
            }
            nlines++;
            while(end < len && text[end] != NEW_LINE) end++;
            //2 options: end == len or end == newline
            if(end != len) end++;//consume newline
            int toCopy = end - start;
            if(toCopy > created.data.length){//both are "sizes"
                created.data = resize(created.data, toCopy + 1);//plus 1 for tack-on below
                created.cdata = resize(created.cdata, toCopy + 1);
            }
            System.arraycopy(text, start, created.data, 0, toCopy);
            created.width = toCopy;
            if(end == len){//tack on newline to last line
                created.data[created.width++] = NEW_LINE;
            }
            start = end;
        }
    }
    //Tested decently(not solid though)
    public void deleteLines(int start, int end){
        int minStart = start <= end ? start : end;
        int maxEnd = start <= end ? end : start;
        if(minStart < 0) minStart = 0;
        if(maxEnd >= nlines) maxEnd = nlines - 1;
        if(minStart == 0 && maxEnd == nlines - 1){
            head = last = arrow = null;
            nlines = 0;
            return;
        }
        Line lstart = nodeForLine(minStart, false);
        Line lend = nodeForLine(maxEnd, false);
        if(lstart == head){
            head = lend.next;
            head.prev = null;
        } else if(lend == last){
            last = lstart.prev;
            last.next = null;
            lstart.prev = null;
            if(last.width > 0 && last.data[last.width - 1] == NEW_LINE){
                last.data[--last.width] = 0;
            }
        } else {
            lstart.prev.next = lend.next;
            lend.next.prev = lstart.prev;
            lstart.prev = null;
            lend.next = null;
        }
        nlines = nlines - (maxEnd - minStart + 1);
    }

//    public boolean deleteLine(int line) {
//        if (head == null || line < 0 || line >= nlines) {
//            return false;
//        }
//        Line cursor = head;
//        if (line < (nlines >> 1)) {
//            for (int i = 0; i < line; i++) {
//                cursor = cursor.next;
//            }
//        } else {
//            cursor = last;
//            for (int i = nlines - 1; i > line; i--) {
//                cursor = cursor.prev;
//            }
//        }
//        if (nlines == 1) {
//            head = last = null;
//            nlines = 0;
//            return true;
//        } else if (cursor == head) {
//            head = cursor.next;
//            cursor.next = null;
//            head.prev = null;
//        } else if (cursor == last) {
//            last = cursor.prev;
//            last.next = null;
//            cursor.prev = null;//for gc
//        } else {
//            //Somewhere in middle
//            cursor.prev.next = cursor.next;
//            cursor.next.prev = cursor.prev;
//            cursor.next = cursor.prev = null;
//        }
//        nlines--;
//        return true;
//    }

    //Support
    /**
     * Call with true to create lines if out of bounds Passing false to expand
     * means that a null-check is required on returned result.
     */
    private Line nodeForLine(int line, boolean expand) {
        //handle infinite length
        if (!expand && line >= nlines) {
            return null;
        }
        while (nlines <= line) {//addLast
            //verify that last initial line has a newline
            if(last != null){//if a last exists
                if(last.width == 0){
                    last.data[last.width++] = NEW_LINE;
                } else if(last.data[last.width - 1] != NEW_LINE){
                    if(last.width >= last.data.length){
                        last.data = resize(last.data, last.data.length + 3);
                        last.cdata = resize(last.cdata, last.cdata.length + 3);
                    }
                    last.data[last.width++] = NEW_LINE;
                }
            }
            Line end = new Line(columnDefaultWidth);
            if (last == null) {//handle empty list
                head = last = arrow = end;
            } else {
                end.prev = last;
                last.next = end;
                last = end;
            }
            nlines++;
        }
        Line cursor = head;
        if (line < (nlines >> 1)) {
            for (int x = 0; x < line; x++) {
                cursor = cursor.next;
            }
        } else {
            cursor = last;
            for (int x = nlines - 1; x > line; x--) {
                cursor = cursor.prev;
            }
        }
        return cursor;
    }

    private static char[] resize(char[] line, int nwsize) {
        char[] copy = new char[nwsize];
        System.arraycopy(line, 0, copy, 0, Math.min(line.length, nwsize));
        return copy;
    }

    private static int[] resize(int[] line, int nwsize) {
        int[] copy = new int[nwsize];
        System.arraycopy(line, 0, copy, 0, Math.min(line.length, nwsize));
        return copy;
    }
    
    //Last Usable Char
    public int getLast(int line){//-1 means there isnt one(line with solitary newline yields -1)
        Line nline = nodeForLine(line, false);
        if(nline == null || nline.width == 0) return -1;
        if(nline.data[nline.width - 1] == NEW_LINE) return nline.width - 2;
        return nline.width - 1;
    }
    //Encodes the output for testing
    public void show(boolean showCapacity) {
        StringBuilder res = new StringBuilder();
        res.append("Size: ").append(nlines).append(" lines\n");
        for (Line n = head; n != null; n = n.next) {
            int end = showCapacity ? n.data.length : n.width;
            res.append(String.format("cap: %2d, wid: %2d ::  ", n.data.length, n.width));
            for (int c = 0; c < end; c++) {
                res.append(n.data[c] == 0 ? '#' : n.data[c] == '\n' ? '$' : n.data[c]);//shows newlines as $
            }
            res.append('\n');//this will double space if there is actually newlines!
        }
        System.out.println(res.toString());
    }
    
    private static class Line {
        //capacity is data.length
        char[] data;
        int[] cdata;
        int width;//num chars loaded
        Line next;
        Line prev;

        public Line(int size) {
            data = new char[size];
            cdata = new int[size];
        }
    }
    public static void main(String[] args) {
//        Model mod = new Model();
//        mod.addLine("hey wiggles worth");
//        mod.appendLine(0, '\n');
//        mod.insert(1, 0, "a");
//        //mod.appendDocument(12, "678");
//        //mod.appendDocument(3, "pimpshit");
//        //mod.appendDocument(3, "birdshit");
//        //mod.appendDocument(3, "crapshit");
//        //mod.insertLine(0, "nix\nray its\nblue");
//        mod.show(true);
//        System.out.println("Last 0: " + mod.getLast(0));
//        System.out.println("Last 1: " + mod.getLast(1));
//        //mod.deleteLines(1, 4);
//        //mod.clear();
//        //mod.show(true);
        HashSet<String> words = new HashSet<>();
        words.add("public");
        words.add("private");
        words.add("int");
        
        String data = "hey public man whats fucking int dats private";
        System.out.println(data.contains("int"));
        System.out.println(data.indexOf("int"));
        
        words.stream().filter(s -> data.contains(s)).map(s -> s.indexOf(s));
        
    }
}
