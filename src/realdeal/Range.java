/**
 *
 * 
 *
 */

package realdeal;

/**
 *
 * @author chris
 */
public class Range {
    public int start;
    public int end;

    public Range(){
        
    }
    public Range(int s, int e){
        start = s;
        end = e;
    }
    public boolean overlaps(Range other){
        int twidth = end - start;
        int owidth = other.end - other.start;
        return (other.start >= start && other.start < start + twidth) && (other.start + owidth >= start && other.start + owidth < start + twidth);
    }
}
