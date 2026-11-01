// SPDX-License-Identifier: GPL-3.0-only
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * StubHitsInstrumenter IN.jar OUT.jar TABLE.tsv
 *
 * Finds the stub methods of ATL's Java classes by their bytecode and puts a call
 * android.atl.ATLStubHits.hit(id, "class\tmethod\tsignature\tkind") at their start (see ATLStubHits). Kinds, named
 * like atl-doctor's stub list:
 *   empty      void method that only returns
 *   empty0     only returns null/0/false, an empty array or Collections.empty*()
 *   emptythis  only returns this
 *   const      only returns a constant
 *   returnsarg only returns one of its parameters
 *   throws     short body that throws UnsupportedOperationException or an exception with a stub text
 *   logs       short body with a stub text (stub, not implemented, unimplemented, TODO, FIXME)
 * Constructors, static initializers, abstract, native, bridge and synthetic methods are left alone. TABLE gets one
 * line per stub: id, class, method, signature, kind.
 */
public class StubHitsInstrumenter {
	static final String HITS = "android/atl/ATLStubHits";
	static final Pattern STUBTEXT = Pattern.compile("stub|not implemented|unimplemented|\\btodo\\b|fixme", Pattern.CASE_INSENSITIVE);
	static final int SHORT = 24; // instructions of a "short" body (atl-doctor: at most 3 statements, no loop)
	static int next = 0;

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("usage: StubHitsInstrumenter IN.jar OUT.jar TABLE.tsv");
			System.exit(2);
		}
		try (ZipFile in = new ZipFile(args[0]);
		     ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]));
		     PrintWriter table = new PrintWriter(args[2], "UTF-8")) {
			for (Enumeration<? extends ZipEntry> e = in.entries(); e.hasMoreElements();) {
				ZipEntry ze = e.nextElement();
				byte[] data = read(in.getInputStream(ze));
				if (ze.getName().endsWith(".class"))
					data = instrument(data, table);
				ZipEntry oe = new ZipEntry(ze.getName());
				oe.setTime(ze.getTime()); // reproducible like the input
				out.putNextEntry(oe);
				out.write(data);
				out.closeEntry();
			}
		}
		System.err.println("stub hits: " + next + " stub methods instrumented");
	}

	static byte[] read(InputStream is) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		byte[] buf = new byte[65536];
		for (int n; (n = is.read(buf)) > 0;)
			b.write(buf, 0, n);
		return b.toByteArray();
	}

	static byte[] instrument(byte[] data, PrintWriter table) {
		ClassNode cn = new ClassNode();
		new ClassReader(data).accept(cn, 0);
		if (cn.name.equals(HITS) || (cn.access & Opcodes.ACC_INTERFACE) != 0 && cn.methods.isEmpty())
			return data;
		boolean changed = false;
		for (MethodNode m : cn.methods) {
			String kind = kind(m);
			if (kind == null)
				continue;
			if (next > 0xFFFF)
				throw new IllegalStateException("more than 65536 stub methods");
			int id = next++;
			String name = cn.name.replace('/', '.') + "\t" + m.name + "\t" + m.desc + "\t" + kind;
			table.println(id + "\t" + name);
			InsnList call = new InsnList();
			call.add(new LdcInsnNode(id));
			call.add(new LdcInsnNode(name));
			call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HITS, "hit", "(ILjava/lang/String;)V", false));
			m.instructions.insert(call); // stack is empty before and after: existing frames stay valid
			changed = true;
		}
		if (!changed)
			return data;
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	/** the instructions that do something (no labels, line numbers, frames) */
	static AbstractInsnNode[] code(MethodNode m) {
		java.util.ArrayList<AbstractInsnNode> l = new java.util.ArrayList<>();
		for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext())
			if (i.getOpcode() >= 0)
				l.add(i);
		return l.toArray(new AbstractInsnNode[0]);
	}

	static String kind(MethodNode m) {
		if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC)) != 0 ||
		    m.name.startsWith("<") || m.instructions.size() == 0)
			return null;
		AbstractInsnNode[] c = code(m);
		boolean stat = (m.access & Opcodes.ACC_STATIC) != 0;
		Type ret = Type.getReturnType(m.desc);
		if (c.length == 1 && c[0].getOpcode() == Opcodes.RETURN)
			return "empty";
		if (c.length == 2 && isReturn(c[1])) {
			AbstractInsnNode v = c[0];
			int op = v.getOpcode();
			if (op == Opcodes.ACONST_NULL || op == Opcodes.ICONST_0 || op == Opcodes.LCONST_0 || op == Opcodes.FCONST_0 ||
			    op == Opcodes.DCONST_0)
				return "empty0";
			if (op == Opcodes.ALOAD && ((VarInsnNode)v).var == 0 && !stat)
				return "emptythis";
			if (v instanceof VarInsnNode && op >= Opcodes.ILOAD && op <= Opcodes.ALOAD)
				return "returnsarg";
			if (v instanceof MethodInsnNode && ((MethodInsnNode)v).owner.equals("java/util/Collections") &&
			    ((MethodInsnNode)v).name.startsWith("empty"))
				return "empty0";
			if ((op >= Opcodes.ICONST_M1 && op <= Opcodes.DCONST_1) || op == Opcodes.BIPUSH || op == Opcodes.SIPUSH ||
			    op == Opcodes.LDC || (op == Opcodes.GETSTATIC && constant((FieldInsnNode)v)))
				return "const";
		}
		if (c.length == 3 && isReturn(c[2]) && c[0].getOpcode() == Opcodes.ICONST_0 &&
		    (c[1].getOpcode() == Opcodes.NEWARRAY || c[1].getOpcode() == Opcodes.ANEWARRAY))
			return "empty0";
		if (c.length > SHORT)
			return null;
		boolean throwsOp = false, unsupported = false, stubText = false, loop = false;
		for (AbstractInsnNode i : c) {
			if (i.getOpcode() == Opcodes.ATHROW)
				throwsOp = true;
			if (i instanceof TypeInsnNode && i.getOpcode() == Opcodes.NEW &&
			    ((TypeInsnNode)i).desc.equals("java/lang/UnsupportedOperationException"))
				unsupported = true;
			if (i instanceof LdcInsnNode && ((LdcInsnNode)i).cst instanceof String &&
			    STUBTEXT.matcher((String)((LdcInsnNode)i).cst).find())
				stubText = true;
			if (i instanceof JumpInsnNode || i.getOpcode() == Opcodes.TABLESWITCH || i.getOpcode() == Opcodes.LOOKUPSWITCH)
				loop = true; // a branch: one part of a longer body, not the whole method
		}
		if (loop)
			return null;
		if (throwsOp && (unsupported || stubText))
			return "throws";
		if (stubText)
			return "logs";
		return null;
	}

	static boolean isReturn(AbstractInsnNode i) {
		return i.getOpcode() >= Opcodes.IRETURN && i.getOpcode() <= Opcodes.ARETURN;
	}

	/** GETSTATIC of an upper-case constant of another class, e.g. Collections.EMPTY_LIST or View.VISIBLE */
	static boolean constant(FieldInsnNode f) {
		return f.name.matches("[A-Z][A-Z0-9_]+");
	}
}
