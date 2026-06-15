package android.print;

public class PrintResultCallbackBypass {
    
    public interface LayoutCallbackImpl {
        void onLayoutFinished(PrintDocumentInfo info, boolean changed);
        void onLayoutFailed(CharSequence error);
        void onLayoutCancelled();
    }

    public interface WriteCallbackImpl {
        void onWriteFinished(PageRange[] pages);
        void onWriteFailed(CharSequence error);
        void onWriteCancelled();
    }

    public static PrintDocumentAdapter.LayoutResultCallback createLayoutCallback(
        final LayoutCallbackImpl impl
    ) {
        return new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                if (impl != null) {
                    impl.onLayoutFinished(info, changed);
                }
            }
            @Override
            public void onLayoutFailed(CharSequence error) {
                if (impl != null) {
                    impl.onLayoutFailed(error);
                }
            }
            @Override
            public void onLayoutCancelled() {
                if (impl != null) {
                    impl.onLayoutCancelled();
                }
            }
        };
    }

    public static PrintDocumentAdapter.WriteResultCallback createWriteCallback(
        final WriteCallbackImpl impl
    ) {
        return new PrintDocumentAdapter.WriteResultCallback() {
            @Override
            public void onWriteFinished(PageRange[] pages) {
                if (impl != null) {
                    impl.onWriteFinished(pages);
                }
            }
            @Override
            public void onWriteFailed(CharSequence error) {
                if (impl != null) {
                    impl.onWriteFailed(error);
                }
            }
            @Override
            public void onWriteCancelled() {
                if (impl != null) {
                    impl.onWriteCancelled();
                }
            }
        };
    }
}
