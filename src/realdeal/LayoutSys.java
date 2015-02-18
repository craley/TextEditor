/**
 *
 *
 *
 */
package realdeal;


/**
 * Forms the model from data, optionally using the viewport's width as a
 * constraint.
 */
public class LayoutSys {

    boolean wordwrap;
    int gap = 2;

    Model model;
    Metrics metrics;
    ScrollSys scroll;

    public LayoutSys() {

    }

    public void setScroll(ScrollSys scr) {
        scroll = scr;
    }

    public void setModel(Model m) {
        model = m;
    }

    public void setMetrics(Metrics m) {
        metrics = m;
    }

    //to initialize: dont care if wrap on or off

    public void initModel(char[] data) {
        int trip = 0;
        int cursor = 0;
        String snag = null;
        while (cursor < data.length) {
            trip = cursor;
            while (cursor < data.length && data[cursor] != '\n') {
                cursor++;
            }
            //cursor points to newline or is equal to length
            if(cursor != data.length) cursor++;
            snag = new String(data, trip, cursor - trip);
            model.addLine(snag.toCharArray());
            setModelColor(snag);
        }
        int maxWidth = 0, currentWidth = 0;
        int climit = 0;
        model.setArrow(0);
        for (int line = 0; line < model.nlines; line++) {
            currentWidth = 0;
            climit = model.arrowWidth();
            for (int col = 0; col < climit; col++) {
                currentWidth += metrics.charmap.get(model.charAtArrow(col));
            }
            if (currentWidth > maxWidth) {
                maxWidth = currentWidth;
            }
            model.next();
        }
        int maxHeight = model.nlines * (metrics.textheight + gap);
        //establish document size in scroller
        //scroll.updateModelDimension(maxWidth, maxHeight);
        scroll.validateFromModel(maxWidth, maxHeight);
    }
    private void setModelColor(final String text){
        model.wordset.stream().filter(word -> text.contains(word)).forEach(wd -> {
            int start = text.indexOf(wd);
            int end = start + wd.length() - 1;
            //without filters it colors: 'int' inside 'Hints'
            if(start - 1 >= 0 && isAlpha(text.charAt(start - 1))) return;
            if(end + 1 < text.length() && isAlpha(text.charAt(end + 1))) return;
            model.setTextColorLast(start, end, model.wordcolor);
        });
    }
    private static boolean isAlpha(char ch){
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    //Terribly inefficient: redoes the whole thing. fixx
    private void relayout() {
        //if(model.nlines == 0) return;
        char[] data = model.getLinearModel();//cant do that everytime!
        int trip = 0;
        int cursor = 0;
        int offset = 0;
        int availWidth = scroll.getAvailableWidth();
        while (cursor < data.length) {
            offset = 0;
            trip = cursor;
            while (cursor < data.length && data[cursor] != '\n' && offset < availWidth) {
                offset += metrics.charmap.get(data[cursor]);
                cursor++;
            }
            if (cursor != data.length) {
                if (data[cursor] == '\n') {
                    cursor++;//so we grab the newline also
                    model.addLine(new String(data, trip, cursor - trip).toCharArray());
                } else {
                    int rev = cursor;
                    while (rev > trip && data[rev] != ' ') {
                        rev--;//backup to find previous space
                    }
                    if (rev == trip) {//there was no space: just break it then
                        //cut off at prev char: dont advance cursor
                        model.addLine(new String(data, trip, cursor - trip).toCharArray());
                    } else {
                        cursor = rev + 1;
                        model.addLine(new String(data, trip, cursor - trip).toCharArray());
                    }
                }
            } else {
                model.addLine(new String(data, trip, cursor - trip).toCharArray());
            }
        }
    }

    public void setLineGap(int g) {
        gap = g;
        revalidate();
    }

    /**
     * Called anytime model changes. 1. Performs a measurement of document 2. If
     * word wrap on and width exceeds, relayout 3. Update document size with
     * scroller.
     */
    public void revalidate() {
        if (model.nlines == 0) {
            //scroll.updateModelDimension(0, 0);
            scroll.validateFromModel(0, 0);
            return;
        }
        int maxWidth = 0, currentWidth = 0;
        int climit = 0;
        boolean exceededWidth = false;
        model.setArrow(0);
        int viewPortwidth = scroll.getAvailableWidth();//will be zero first time
        for (int line = 0; line < model.nlines; line++) {
            currentWidth = 0;
            climit = model.arrowWidth();
            for (int col = 0; col < climit; col++) {
                currentWidth += metrics.charmap.get(model.charAtArrow(col));
            }
            if (!exceededWidth && currentWidth > viewPortwidth) {
                exceededWidth = true;
            }
            if (currentWidth > maxWidth) {
                maxWidth = currentWidth;
            }
            model.next();
        }
        int maxHeight = model.nlines * (metrics.textheight + gap);
        if (exceededWidth && wordwrap && viewPortwidth > 0) {//catches first time when camwidth = 0
            relayout();
            revalidate();//recurse
        } else {
            //scroll.updateModelDimension(maxWidth, maxHeight);
            scroll.validateFromModel(maxWidth, maxHeight);
        }

    }
}
