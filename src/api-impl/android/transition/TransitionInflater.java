package android.transition;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* AOSP's TransitionInflater.inflateTransition()/createTransitionFromXml()/createCustom(): the tags of the
 * transitions that ATL has (fade, slide, changeBounds, transitionSet, transition class=...); the
 * <targets> of a transition are skipped, and there is no TransitionManager */
public class TransitionInflater {
	private static final Class<?>[] sConstructorSignature = new Class[] {Context.class, AttributeSet.class};
	private static final HashMap<String, Constructor> sConstructors = new HashMap<String, Constructor>();

	private Context mContext;

	private TransitionInflater(Context context) {
		mContext = context;
	}

	public static TransitionInflater from(Context context) {
		return new TransitionInflater(context);
	}

	public Transition inflateTransition(int resource) {
		XmlResourceParser parser = mContext.getResources().getXml(resource);
		try {
			return createTransitionFromXml(parser, Xml.asAttributeSet(parser), null);
		} catch (XmlPullParserException e) {
			InflateException ex = new InflateException(e.getMessage(), e);
			throw ex;
		} catch (IOException e) {
			throw new InflateException(parser.getPositionDescription() + ": " + e.getMessage(), e);
		} finally {
			parser.close();
		}
	}

	private Transition createTransitionFromXml(XmlPullParser parser, AttributeSet attrs, Transition parent) throws XmlPullParserException, IOException {
		Transition transition = null;

		// Make sure we are on a start tag.
		int type;
		int depth = parser.getDepth();

		TransitionSet transitionSet = (parent instanceof TransitionSet) ? (TransitionSet)parent : null;

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			String name = parser.getName();
			if ("fade".equals(name)) {
				transition = new Fade(mContext, attrs);
			} else if ("changeBounds".equals(name)) {
				transition = new ChangeBounds(mContext, attrs);
			} else if ("slide".equals(name)) {
				transition = new Slide(mContext, attrs);
			} else if ("transitionSet".equals(name)) {
				transition = new TransitionSet(mContext, attrs);
			} else if ("transition".equals(name)) {
				transition = (Transition)createCustom(attrs, Transition.class, "transition");
			} else if ("targets".equals(name)) {
				skipElement(parser);
				continue;
			} else {
				throw new RuntimeException("Unknown transition name: " + parser.getName());
			}
			if (transition != null) {
				if (!parser.isEmptyElementTag()) {
					createTransitionFromXml(parser, attrs, transition);
				}
				if (transitionSet != null) {
					transitionSet.addTransition(transition);
					transition = null;
				} else if (parent != null) {
					throw new InflateException("Could not add transition to another transition.");
				}
			}
		}
		return transition;
	}

	private void skipElement(XmlPullParser parser) throws XmlPullParserException, IOException {
		int depth = parser.getDepth();
		while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > depth) {
			if (parser.getEventType() == XmlPullParser.END_DOCUMENT)
				return;
		}
	}

	private Object createCustom(AttributeSet attrs, Class expectedType, String tag) {
		String className = attrs.getAttributeValue(null, "class");

		if (className == null) {
			throw new InflateException(tag + " tag must have a 'class' attribute");
		}

		try {
			synchronized (sConstructors) {
				Constructor constructor = sConstructors.get(className);
				if (constructor == null) {
					Class c = mContext.getClassLoader().loadClass(className).asSubclass(expectedType);
					if (c != null) {
						constructor = c.getConstructor(sConstructorSignature);
						constructor.setAccessible(true);
						sConstructors.put(className, constructor);
					}
				}
				return constructor.newInstance(mContext, attrs);
			}
		} catch (Exception e) {
			throw new InflateException("Could not instantiate " + expectedType + " class " + className, e);
		}
	}
}
