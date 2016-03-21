package org.beetlebug;

import java.util.UUID;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.extension.IClassSpecificNameResolver;
import org.eclipse.mat.snapshot.extension.Subject;
import org.eclipse.mat.snapshot.model.IObject;

@Subject("java.util.UUID")
public class UUIDNameResolver implements IClassSpecificNameResolver {

    @Override
    public String resolve(IObject object) throws SnapshotException {
        Long most = (Long) object.resolveValue("mostSigBits");
        Long least = (Long) object.resolveValue("leastSigBits");

        if (most != null && least != null) {
            return new UUID(most, least).toString();
        }

        return null;
    }

}
