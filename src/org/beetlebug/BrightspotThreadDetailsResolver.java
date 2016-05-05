package org.beetlebug;

import java.util.Date;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.results.TextResult;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.extension.IThreadDetailsResolver;
import org.eclipse.mat.snapshot.extension.IThreadInfo;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IStackFrame;
import org.eclipse.mat.snapshot.model.IThreadStack;
import org.eclipse.mat.snapshot.model.PrettyPrinter;
import org.eclipse.mat.util.IProgressListener;

public class BrightspotThreadDetailsResolver implements IThreadDetailsResolver {

    @Override
    public void complementDeep(IThreadInfo thread, IProgressListener listener) throws SnapshotException {
        ISnapshot snapshot = thread.getThreadObject().getSnapshot();

        IThreadStack stack = snapshot.getThreadStack(thread.getThreadId());
        if (stack == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("<style>.record { padding: 5px; }</style><pre>");

        IObject threadObject = snapshot.getObject(thread.getThreadId());
        builder.append(threadObject.getClassSpecificName()).append("\r\n");
        for (IStackFrame frame : stack.getStackFrames()) {

            if (frame.getText().startsWith("at com.psddev.dari.db.State")
                    || frame.getText().startsWith("at com.psddev.dari.util.AbstractFilter")
                    || frame.getText().startsWith("at com.psddev.cms.db.RichTextDatabase")
                    || frame.getText().startsWith("at com.psddev.dari.db.ForwardingDatabase")) {
                continue;
            }

            builder.append("  ").append(frame.getText()).append("\r\n");

            // 0x3782c7d50
            for (int objectId : frame.getLocalObjectsIds()) {
                IObject local = snapshot.getObject(objectId);
                IClass k = local.getClazz();
                if (k.doesExtend("com.psddev.dari.db.Record")) {
                    builder.append("<div class='record'>");

                    IObject state = (IObject) local.resolveValue("state");
                    if (state != null) {
                        IObject id = (IObject) state.resolveValue("id");
                        String uuid = new UUIDNameResolver().resolve(id);
                        String hex = Long.toHexString(local.getObjectAddress());
                        builder.append("        ");
                        builder.append(String.format("<a href='mat://object/0x%s'>0x%s</a> <b>%s : %s </b><br/>", hex,
                                hex, k.getName(), uuid));
                    } else {
                        builder.append("        ");
                        builder.append(String.format("%s\n", k.getName()));
                    }

                    builder.append("</div>");
                } else if (k.doesExtend("org.apache.coyote.Request")) {
                    builder.append("<div class='record'>");

                    Date startTime = new Date((Long) local.resolveValue("startTime"));

					builder.append("        URL: <b>");
                    IObject serverName = (IObject) local.resolveValue("serverNameMB");
                    if (serverName.resolveValue("strValue") != null) {
						builder.append(PrettyPrinter.objectAsString((IObject) serverName.resolveValue("strValue"), 1024));
                    }

                    IObject uri = (IObject) local.resolveValue("uriMB");
                    if (uri.resolveValue("strValue") != null) {
                    	builder.append(PrettyPrinter.objectAsString((IObject) uri.resolveValue("strValue"), 1024));
                    }

                    IObject query = (IObject) local.resolveValue("queryMB");
                    if (query.resolveValue("strValue") != null) {
                        builder.append("?");
                        builder.append(PrettyPrinter.objectAsString((IObject) query.resolveValue("strValue"), 1024));
                    }

                    builder.append("</b></div>");

                    builder.append("<div class='record'>");
                    builder.append("        Time: <b>" + startTime.toString());
                    builder.append("</b></div>");

                    break;
                }
            }
        }

        builder.append("</pre>");

        thread.addDetails("Brightspot Annotated Thread Information", new TextResult(builder.toString(), true));
    }

    @Override
    public void complementShallow(IThreadInfo thread, IProgressListener listener) throws SnapshotException {

    }

    @Override
    public Column[] getColumns() {
        return null;
    }
}
