package android.text;

public interface TextDirectionHeuristic {

	public boolean isRtl(char[] array, int start, int count);

	public boolean isRtl(CharSequence text, int start, int end);
}
