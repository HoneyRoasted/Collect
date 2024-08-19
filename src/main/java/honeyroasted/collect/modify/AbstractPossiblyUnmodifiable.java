package honeyroasted.collect.modify;

public abstract class AbstractPossiblyUnmodifiable implements PossiblyUnmodifiable {
    private boolean unmodifiable;

    protected void checkUnmodifiable() {
        if (this.unmodifiable) {
            throw new UnsupportedOperationException("This object is currently unmodifiable");
        }
    }

    protected void makeUnmodifiable() {
    }

    protected void makeModifiable() {
    }

    public boolean isUnmodifiable() {
        return this.unmodifiable;
    }

    public void setUnmodifiable(boolean unmodifiable) {
        if (!this.unmodifiable && unmodifiable) {
            makeUnmodifiable();
        } else if (this.unmodifiable && !unmodifiable) {
            makeModifiable();
        }
        this.unmodifiable = unmodifiable;
    }
}
