package gov.nist.toolkit.fhir.simulators.sim.reg.store.resource;

/**
 *
 */
public class ResourceCollection {
    transient private boolean dirty;
    transient private ResourceIndex index;
    transient private ResourceCollection parent = null;

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() { return dirty; }

    public void init() {
        dirty = false;
    }

    public void setIndex(ResourceIndex index) { this.index = index; }

    public void clear() {}
}
