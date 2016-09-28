package co.smartreceipts.android.sync;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.database.controllers.TableControllerManager;
import co.smartreceipts.android.sync.model.RemoteBackupMetadata;
import co.smartreceipts.android.sync.model.impl.Identifier;
import co.smartreceipts.android.sync.provider.SyncProvider;
import co.smartreceipts.android.sync.provider.SyncProviderFactory;
import co.smartreceipts.android.sync.provider.SyncProviderStore;
import rx.Observable;

/**
 * A global manager for whatever our current backup provider may (or may not) be
 */
public class BackupProvidersManager implements BackupProvider {

    private final SyncProviderFactory mSyncProviderFactory;
    private final SyncProviderStore mSyncProviderStore;
    private final Set<BackupProviderChangeListener> mBackupProviderChangeListeners = new CopyOnWriteArraySet<>();
    private BackupProvider mBackupProvider;

    public BackupProvidersManager(@NonNull Context context, @NonNull DatabaseHelper databaseHelper, @NonNull TableControllerManager tableControllerManager) {
        this(new SyncProviderFactory(context, databaseHelper, tableControllerManager), new SyncProviderStore(context));
    }

    public BackupProvidersManager(@NonNull SyncProviderFactory syncProviderFactory, @NonNull SyncProviderStore syncProviderStore) {
        mSyncProviderFactory = Preconditions.checkNotNull(syncProviderFactory);
        mSyncProviderStore = Preconditions.checkNotNull(syncProviderStore);
        mBackupProvider = syncProviderFactory.get(mSyncProviderStore.getProvider());
    }

    public void registerChangeListener(@NonNull BackupProviderChangeListener backupProviderChangeListener) {
        Preconditions.checkNotNull(backupProviderChangeListener);
        mBackupProviderChangeListeners.add(backupProviderChangeListener);
    }

    public void unregisterChangeListener(@NonNull BackupProviderChangeListener backupProviderChangeListener) {
        Preconditions.checkNotNull(backupProviderChangeListener);
        mBackupProviderChangeListeners.remove(backupProviderChangeListener);
    }

    public synchronized void setAndInitializeSyncProvider(@NonNull SyncProvider syncProvider, @Nullable FragmentActivity fragmentActivity) {
        if (mSyncProviderStore.setSyncProvider(syncProvider)) {
            mBackupProvider.deinitialize();
            mBackupProvider = mSyncProviderFactory.get(syncProvider);
            mBackupProvider.initialize(fragmentActivity);
            for (final BackupProviderChangeListener backupProviderChangeListener : mBackupProviderChangeListeners) {
                backupProviderChangeListener.onProviderChanged(syncProvider);
            }
        }
    }

    @NonNull
    public SyncProvider getSyncProvider() {
        return mSyncProviderStore.getProvider();
    }

    @Override
    public synchronized void initialize(@Nullable FragmentActivity activity) {
        mBackupProvider.initialize(activity);
    }

    @Override
    public synchronized void deinitialize() {
        mBackupProvider.deinitialize();
    }

    @Override
    public synchronized boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        return mBackupProvider.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public Observable<List<RemoteBackupMetadata>> getRemoteBackups() {
        return mBackupProvider.getRemoteBackups();
    }

    @Nullable
    @Override
    public Identifier getDeviceSyncId() {
        return mBackupProvider.getDeviceSyncId();
    }
}