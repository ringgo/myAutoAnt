package org.msf.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MJavac extends Javac {
	public static final String FILE_SEPARATOR = System
			.getProperty("file.separator");
	public static final String MJAVAC_PROPERTY_SRC = "mjavac_srcs";
	public static final String MJAVAC_PROPERTY_LIB = "mjavac_libs";
	/**
	private static final String ECLIPSE_SRC = "src";
	private static final String ECLIPSE_CON = "con";
	private static final String ECLIPSE_LIB = "lib";
	private static final String ECLIPSE_OUTPUT = "output";
	*/
	private static String _projectxmlfile;
	private static String _classpathxmlfile;
	private String workspace;
	private String projectName;
	private String destlib;
	private StringBuffer srcbuf = new StringBuffer();
	private StringBuffer libbuf = new StringBuffer();
	private FileUtils fileUtils;

	public MJavac() {
		this.fileUtils = FileUtils.getFileUtils();
	}

	public String getDestlib() {
		return this.destlib;
	}

	public void setDestlib(String destlib) {
		this.destlib = destlib;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getWorkspace() {
		return this.workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	private static DocumentBuilder getDocumentBuilder() {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (Exception exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}

	public void execute() throws BuildException {
		try {
			File libdir = new File(this.destlib);
			if ((libdir.isDirectory()) && (libdir.exists())) {
				proClassPathXml(this.projectName);
				super.execute();
				copylibs(libdir);
				copyOthersrcs();
			} else {
				throw new BuildException("destlib error!");
			}

			if (getProject().getProperties().containsKey("mjavac_srcs")) {
				getProject().setProperty("mjavac_srcs",
						this.srcbuf.toString().replace(';', ','));
			}

			if (getProject().getProperties().containsKey("mjavac_libs"))
				getProject().setProperty("mjavac_libs",
						this.libbuf.toString().replace(';', ','));
		} catch (Exception x) {
			if (x != null) {
				throw new BuildException(x.getMessage());
			}
			throw new BuildException("null..");
		}
	}

	private void copylibs(File libdir) throws IOException {
		String[] libs = this.libbuf.toString().split(";");
		for (String lib : libs) {
			System.out.println("copy lib files:" + lib);
			this.fileUtils.copyFile(lib, libdir.getAbsolutePath()
					+ FILE_SEPARATOR + new File(lib).getName(), null, true);
		}
	}

	private void copyOthersrcs() throws IOException {
		String[] srcs = this.srcbuf.toString().split(";");
		for (String src : srcs) {
			System.out.println("copy src files:" + src);
			proCopyOtherFiles(src, src.replace("/", FILE_SEPARATOR));
		}
	}

	private void proCopyOtherFiles(String fpath, String rootsrc)
			throws IOException {
		File[] fs = new File(fpath).listFiles();
		for (File f : fs)
			if (f.isDirectory()) {
				if (!(".svn".equalsIgnoreCase(f.getName()))) {
					proCopyOtherFiles(f.getAbsolutePath(), rootsrc);
				}
			} else if (!(f.getName().endsWith(".java")))
				this.fileUtils.copyFile(f.getAbsolutePath(), getDestdir()
						.getAbsolutePath()
						+ f.getAbsolutePath().replace(rootsrc, ""), null, true);
	}

	private void proClassPathXml(String prohectNm) {
		try {
			String projectdir = this.workspace + prohectNm;
			_projectxmlfile = projectdir + FILE_SEPARATOR + ".project";
			_classpathxmlfile = projectdir + FILE_SEPARATOR + ".classpath";

			Document doc = getDocumentBuilder().parse(
					new File(_classpathxmlfile));
			NodeList classpathentrys = doc
					.getElementsByTagName("classpathentry");

			for (int i = 0; i < classpathentrys.getLength(); ++i) {
				Node tmp = classpathentrys.item(i);
				String kind = getAttributeValue(tmp, "kind");
				String path = getAttributeValue(tmp, "path");

				if ("src".equalsIgnoreCase(kind)) {
					boolean bol = false;
					if (new File(projectdir + FILE_SEPARATOR + path).exists()) {
						System.out.println("currsrc:" + projectdir
								+ FILE_SEPARATOR + path);
						this.srcbuf.append(projectdir + FILE_SEPARATOR + path);
						bol = true;
					} else if (path.startsWith("/")) {
						System.out.println("project:" + this.workspace + path);
						proClassPathXml(path);
					} else {
						System.out.println("linksrc:" + getLinkedValue(path));
						this.srcbuf.append(getLinkedValue(path));
						bol = true;
					}

					if (bol) {
						this.srcbuf.append(";");
					}
				}

				if ("lib".equalsIgnoreCase(kind)) {
					System.out.println("lib:" + path);
					if (path.startsWith("/"))
						this.libbuf.append(this.workspace + path + ";");
					else if (path.indexOf(":") != -1)
						this.libbuf.append(path + ";");
					else {
						this.libbuf.append(projectdir + FILE_SEPARATOR + path
								+ ";");
					}
				}
			}
			setSrcdir(new Path(null, this.srcbuf.toString()));
			setClasspath(new Path(null, this.libbuf.toString()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getLinkedValue(String linkNm) throws Exception {
		Document doc = getDocumentBuilder().parse(new File(_projectxmlfile));
		NodeList links = doc.getElementsByTagName("link");
		for (int i = 0; i < links.getLength(); ++i) {
			String nmvl = getNodeValueByTagNm(links.item(i), "name");
			if ((nmvl != null) && (nmvl.equalsIgnoreCase(linkNm))) {
				return getNodeValueByTagNm(links.item(i), "location");
			}
		}
		return null;
	}

	private static String getNodeValueByTagNm(Node parentNode, String subTagNm) {
		NodeList nl = parentNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); ++i) {
			Node tmp = nl.item(i);
			if (tmp.getNodeName().equalsIgnoreCase(subTagNm)) {
				return tmp.getTextContent();
			}
		}
		return null;
	}

	private static String getAttributeValue(Node node, String attNm) {
		return node.getAttributes().getNamedItem(attNm).getNodeValue();
	}
}