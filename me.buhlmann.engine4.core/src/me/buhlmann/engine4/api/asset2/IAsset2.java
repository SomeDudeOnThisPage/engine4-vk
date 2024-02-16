package me.buhlmann.engine4.api.asset2;

import me.buhlmann.engine4.api.IDisposable;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public interface IAsset2 extends IDisposable
{
    class IAssetReference extends PhantomReference<IAsset2> {

        /**
         * Creates a new phantom reference that refers to the given object and
         * is registered with the given queue.
         *
         * <p> It is possible to create a phantom reference with a {@code null}
         * queue, but such a reference is completely useless: Its {@code get}
         * method will always return {@code null} and, since it does not have a queue,
         * it will never be enqueued.
         *
         * @param referent the object the new phantom reference will refer to
         * @param q        the queue with which the reference is to be registered,
         *                 or {@code null} if registration is not required
         */
        public IAssetReference(final IAsset2 referent, final ReferenceQueue<? super IAsset2> q)
        {
            super(referent, q);
        }

        public void dispose()
        {
            this.get().dispose();
        }
    }


}
