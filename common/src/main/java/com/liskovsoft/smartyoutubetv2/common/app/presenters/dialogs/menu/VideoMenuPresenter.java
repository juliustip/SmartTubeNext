package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu;

import android.content.Context;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.data.PlaylistInfo;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxUtils;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.misc.StreamReminderService;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.lang.ref.WeakReference;
import java.util.List;

public class VideoMenuPresenter extends BaseMenuPresenter {
    private static final String TAG = VideoMenuPresenter.class.getSimpleName();
    private final MediaItemService mItemManager;
    private final AppDialogPresenter mDialogPresenter;
    private final MediaServiceManager mServiceManager;
    private Disposable mAddToPlaylistAction;
    private Disposable mNotInterestedAction;
    private Disposable mSubscribeAction;
    private Disposable mPlaylistsInfoAction;
    private Video mVideo;
    public static WeakReference<Video> sVideoHolder = new WeakReference<>(null);
    private boolean mIsNotInterestedButtonEnabled;
    private boolean mIsRemoveFromHistoryButtonEnabled;
    private boolean mIsOpenChannelButtonEnabled;
    private boolean mIsOpenChannelUploadsButtonEnabled;
    private boolean mIsSubscribeButtonEnabled;
    private boolean mIsShareLinkButtonEnabled;
    private boolean mIsShareEmbedLinkButtonEnabled;
    private boolean mIsAddToPlaylistButtonEnabled;
    private boolean mIsAddToRecentPlaylistButtonEnabled;
    private boolean mIsReturnToBackgroundVideoEnabled;
    private boolean mIsOpenPlaylistButtonEnabled;
    private boolean mIsAddToPlaybackQueueButtonEnabled;
    private boolean mIsShowPlaybackQueueButtonEnabled;
    private boolean mIsOpenDescriptionButtonEnabled;
    private boolean mIsPlayVideoButtonEnabled;
    private boolean mIsPlaylistOrderButtonEnabled;
    private boolean mIsStreamReminderButtonEnabled;
    private VideoMenuCallback mCallback;
    private List<PlaylistInfo> mPlaylistInfos;

    public interface VideoMenuCallback {
        int ACTION_UNDEFINED = 0;
        int ACTION_UNSUBSCRIBE = 1;
        int ACTION_REMOVE = 2;
        int ACTION_REMOVE_FROM_PLAYLIST = 3;
        int ACTION_REMOVE_FROM_QUEUE = 4;
        int ACTION_ADD_TO_QUEUE = 5;
        void onItemAction(Video videoItem, int action);
    }

    private VideoMenuPresenter(Context context) {
        super(context);
        MediaService service = YouTubeMediaService.instance();
        mItemManager = service.getMediaItemService();
        mServiceManager = MediaServiceManager.instance();
        mDialogPresenter = AppDialogPresenter.instance(context);
    }

    public static VideoMenuPresenter instance(Context context) {
        return new VideoMenuPresenter(context);
    }

    @Override
    protected Video getVideo() {
        return mVideo;
    }

    @Override
    protected AppDialogPresenter getDialogPresenter() {
        return mDialogPresenter;
    }

    @Override
    protected VideoMenuCallback getCallback() {
        return mCallback;
    }

    public void showMenu(Video video, VideoMenuCallback callback) {
        mCallback = callback;
        showMenu(video);
    }

    public void showMenu(Video video) {
        if (video == null) {
            return;
        }

        updateEnabledMenuItems();

        RxUtils.disposeActions(mAddToPlaylistAction, mNotInterestedAction, mSubscribeAction);

        mVideo = video;
        sVideoHolder = new WeakReference<>(video);

        MediaServiceManager.instance().authCheck(this::bootstrapPrepareAndShowDialogSigned, this::prepareAndShowDialogUnsigned);
    }

    private void bootstrapPrepareAndShowDialogSigned() {
        mPlaylistInfos = null;
        RxUtils.disposeActions(mPlaylistsInfoAction);
        if (isAddToRecentPlaylistButtonEnabled()) {
            mPlaylistsInfoAction = mItemManager.getPlaylistsInfoObserve(mVideo.videoId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            videoPlaylistInfos -> {
                                mPlaylistInfos = videoPlaylistInfos;
                                prepareAndShowDialogSigned();
                            },
                            error -> Log.e(TAG, "Add to recent playlist error: %s", error.getMessage())
                    );
        } else {
            prepareAndShowDialogSigned();
        }
    }

    private void prepareAndShowDialogSigned() {
        if (getContext() == null) {
            return;
        }

        mDialogPresenter.clear();

        appendReturnToBackgroundVideoButton();
        appendPlayVideoButton();
        //appendNotInterestedButton();
        appendStreamReminderButton();
        appendRemoveFromHistoryButton();
        appendAddToRecentPlaylistButton();
        appendAddToPlaylistButton();
        appendCreatePlaylistButton();
        appendAddToNewPlaylistButton();
        appendNotInterestedButton();
        appendRenamePlaylistButton();
        appendPlaylistOrderButton();
        appendAddToPlaybackQueueButton();
        appendShowPlaybackQueueButton();
        appendOpenChannelButton();
        //appendOpenChannelUploadsButton();
        appendOpenPlaylistButton();
        appendSubscribeButton();
        appendTogglePinVideoToSidebarButton();
        appendSaveRemovePlaylistButton();
        appendOpenDescriptionButton();
        appendShareLinkButton();
        appendShareEmbedLinkButton();
        appendAccountSelectionButton();
        appendToggleHistoryButton();
        appendClearHistoryButton();

        if (!mDialogPresenter.isEmpty()) {
            String title = mVideo != null ? mVideo.title : null;
            // No need to add author because: 1) This could be a channel card. 2) This info isn't so important.
            mDialogPresenter.showDialog(title);
        }
    }

    private void prepareAndShowDialogUnsigned() {
        if (getContext() == null) {
            return;
        }

        mDialogPresenter.clear();

        appendReturnToBackgroundVideoButton();
        appendPlayVideoButton();
        appendAddToPlaybackQueueButton();
        appendShowPlaybackQueueButton();
        appendOpenChannelButton();
        appendOpenPlaylistButton();
        appendTogglePinVideoToSidebarButton();
        appendOpenDescriptionButton();
        appendShareLinkButton();
        appendShareEmbedLinkButton();
        appendAccountSelectionButton();

        if (mDialogPresenter.isEmpty()) {
            MessageHelpers.showMessage(getContext(), R.string.msg_signed_users_only);
        } else {
            mDialogPresenter.showDialog(mVideo.title);
        }
    }

    private void appendAddToPlaylistButton() {
        if (!mIsAddToPlaylistButtonEnabled) {
            return;
        }

        if (mVideo == null || !mVideo.hasVideo() || mVideo.isPlaylistAsChannel()) {
            return;
        }

        getDialogPresenter().appendSingleButton(
                UiOptionItem.from(
                        getContext().getString(R.string.dialog_add_to_playlist),
                        optionItem -> AppDialogUtil.showAddToPlaylistDialog(getContext(), mVideo, mCallback)
                ));
    }

    private void appendAddToRecentPlaylistButton() {
        if (!isAddToRecentPlaylistButtonEnabled()) {
            return;
        }

        String playlistId = GeneralData.instance(getContext()).getLastPlaylistId();
        String playlistTitle = GeneralData.instance(getContext()).getLastPlaylistTitle();

        if (playlistId == null || playlistTitle == null) {
            return;
        }

        appendSimpleAddToRecentPlaylistButton(playlistId, playlistTitle);
    }

    private boolean isAddToRecentPlaylistButtonEnabled() {
        return mIsAddToPlaylistButtonEnabled && mIsAddToRecentPlaylistButtonEnabled && mVideo != null && mVideo.hasVideo();
    }

    private void appendSimpleAddToRecentPlaylistButton(String playlistId, String playlistTitle) {
        if (mPlaylistInfos == null) {
            return;
        }

        boolean isSelected = false;
        for (PlaylistInfo playlistInfo : mPlaylistInfos) {
            if (playlistInfo.getPlaylistId().equals(playlistId)) {
                isSelected = playlistInfo.isSelected();
                break;
            }
        }
        boolean add = !isSelected;
        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(
                        add ? R.string.dialog_add_to : R.string.dialog_remove_from, playlistTitle),
                        optionItem -> addRemoveFromPlaylist(playlistId, playlistTitle, add)
                )
        );
    }

    //private void appendReactiveAddToRecentPlaylistButton(String playlistId, String playlistTitle) {
    //    mDialogPresenter.appendSingleButton(
    //            UiOptionItem.from(getContext().getString(
    //                    R.string.dialog_add_remove_from, playlistTitle),
    //                    optionItem -> {
    //                        mPlaylistsInfoAction = mItemManager.getVideoPlaylistsInfoObserve(mVideo.videoId)
    //                                .subscribeOn(Schedulers.io())
    //                                .observeOn(AndroidSchedulers.mainThread())
    //                                .subscribe(
    //                                        videoPlaylistInfos -> {
    //                                            for (VideoPlaylistInfo playlistInfo : videoPlaylistInfos) {
    //                                                if (playlistInfo.getPlaylistId().equals(playlistId)) {
    //                                                    addRemoveFromPlaylist(playlistInfo.getPlaylistId(), playlistInfo.getTitle(), !playlistInfo.isSelected());
    //                                                    break;
    //                                                }
    //                                            }
    //                                        },
    //                                        error -> {
    //                                            // Fallback to something on error
    //                                            Log.e(TAG, "Add to recent playlist error: %s", error.getMessage());
    //                                        }
    //                                );
    //                    }
    //            )
    //    );
    //}

    private void appendOpenChannelButton() {
        if (!mIsOpenChannelButtonEnabled) {
            return;
        }

        if (!ChannelPresenter.canOpenChannel(mVideo)) {
            return;
        }

        // Prepare to special type of channels that work as playlist
        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(
                        mVideo.isPlaylistAsChannel() ? R.string.open_playlist : R.string.open_channel), optionItem -> Utils.chooseChannelPresenter(getContext(), mVideo)));
    }

    private void appendOpenPlaylistButton() {
        if (!mIsOpenPlaylistButtonEnabled) {
            return;
        }

        // Check view to allow open playlist in grid
        if (mVideo == null || !mVideo.hasPlaylist() || (mVideo.belongsToSamePlaylistGroup() && ViewManager.instance(getContext()).getTopView() == ChannelUploadsView.class)) {
            return;
        }

        // Prepare to special type of channels that work as playlist
        if (mVideo.isPlaylistAsChannel() && ChannelPresenter.canOpenChannel(mVideo)) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.open_playlist), optionItem -> ChannelUploadsPresenter.instance(getContext()).openChannel(mVideo)));
    }

    private void appendOpenChannelUploadsButton() {
        if (!mIsOpenChannelUploadsButtonEnabled || mVideo == null) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.open_channel_uploads), optionItem -> ChannelUploadsPresenter.instance(getContext()).openChannel(mVideo)));
    }

    private void appendNotInterestedButton() {
        if (mVideo == null || mVideo.mediaItem == null || mVideo.mediaItem.getFeedbackToken() == null) {
            return;
        }

        if (mVideo.belongsToHistory() || !mIsNotInterestedButtonEnabled) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.not_interested), optionItem -> {
                    mNotInterestedAction = mItemManager.markAsNotInterestedObserve(mVideo.mediaItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    var -> {},
                                    error -> Log.e(TAG, "Mark as 'not interested' error: %s", error.getMessage()),
                                    () -> {
                                        if (mCallback != null) {
                                            mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_REMOVE);
                                        } else {
                                            MessageHelpers.showMessage(getContext(), R.string.you_wont_see_this_video);
                                        }
                                    }
                            );
                    mDialogPresenter.closeDialog();
                }));
    }

    private void appendRemoveFromHistoryButton() {
        if (mVideo == null || mVideo.mediaItem == null || mVideo.mediaItem.getFeedbackToken() == null) {
            return;
        }

        if (!mVideo.belongsToHistory() || !mIsRemoveFromHistoryButtonEnabled) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.remove_from_history), optionItem -> {
                    mNotInterestedAction = mItemManager.markAsNotInterestedObserve(mVideo.mediaItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    var -> {},
                                    error -> Log.e(TAG, "Remove from history error: %s", error.getMessage()),
                                    () -> {
                                        if (mCallback != null) {
                                            mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_REMOVE);
                                        } else {
                                            MessageHelpers.showMessage(getContext(), R.string.removed_from_history);
                                        }
                                    }
                            );
                    mDialogPresenter.closeDialog();
                }));
    }

    private void appendShareLinkButton() {
        if (!mIsShareLinkButtonEnabled) {
            return;
        }

        AppDialogUtil.appendShareLinkDialogItem(getContext(), mDialogPresenter, mVideo);
    }

    private void appendShareEmbedLinkButton() {
        if (!mIsShareEmbedLinkButtonEnabled) {
            return;
        }

        AppDialogUtil.appendShareEmbedLinkDialogItem(getContext(), mDialogPresenter, mVideo);
    }

    private void appendOpenDescriptionButton() {
        if (!mIsOpenDescriptionButtonEnabled || mVideo == null) {
            return;
        }

        if (mVideo.videoId == null) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.action_video_info),
                        optionItem -> {
                            MessageHelpers.showMessage(getContext(), R.string.wait_data_loading);
                            mServiceManager.loadMetadata(mVideo, metadata -> {
                                String description = metadata.getDescription();
                                if (description != null) {
                                    showLongTextDialog(description);
                                } else {
                                    mServiceManager.loadFormatInfo(mVideo, formatInfo -> {
                                        String newDescription = formatInfo.getDescription();
                                        if (newDescription != null) {
                                            showLongTextDialog(newDescription);
                                        } else {
                                            MessageHelpers.showMessage(getContext(), R.string.description_not_found);
                                        }
                                    });
                                }
                            });
                        }
                ));
    }

    private void appendPlayVideoButton() {
        if (!mIsPlayVideoButtonEnabled || mVideo == null) {
            return;
        }

        if (mVideo.videoId == null) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.play_video),
                        optionItem -> {
                            PlaybackPresenter.instance(getContext()).openVideo(mVideo);
                            mDialogPresenter.closeDialog();
                        }
                ));
    }

    private void showLongTextDialog(String description) {
        mDialogPresenter.clear();
        mDialogPresenter.appendLongTextCategory(mVideo.title, UiOptionItem.from(description));
        mDialogPresenter.showDialog(mVideo.title);
    }

    private void appendSubscribeButton() {
        if (!mIsSubscribeButtonEnabled) {
            return;
        }

        if (mVideo == null || mVideo.isPlaylistAsChannel() || (!mVideo.canSubscribe() && !mVideo.hasVideo())) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(
                        mVideo.isSubscribed || mVideo.belongsToSubscriptions() || mVideo.belongsToChannelUploads() ?
                                R.string.unsubscribe_from_channel : R.string.subscribe_unsubscribe_from_channel),
                        optionItem -> toggleSubscribe()));
    }

    private void appendReturnToBackgroundVideoButton() {
        if (!mIsReturnToBackgroundVideoEnabled || !PlaybackPresenter.instance(getContext()).isRunningInBackground()) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(R.string.return_to_background_video),
                        // Assume that the Playback view already blocked and remembered.
                        optionItem -> ViewManager.instance(getContext()).startView(PlaybackView.class)
                )
        );
    }

    private void appendAddToPlaybackQueueButton() {
        if (!mIsAddToPlaybackQueueButtonEnabled) {
            return;
        }

        if (mVideo == null || !mVideo.hasVideo()) {
            return;
        }

        Playlist playlist = Playlist.instance();
        // Toggle between add/remove while dialog is opened
        boolean containsVideo = playlist.containsAfterCurrent(mVideo);

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(
                        getContext().getString(containsVideo ? R.string.remove_from_playback_queue : R.string.add_to_playback_queue),
                        optionItem -> {
                            if (containsVideo) {
                                playlist.remove(mVideo);
                                if (mCallback != null) {
                                    mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_REMOVE_FROM_QUEUE);
                                }
                            } else {
                                mVideo.fromQueue = true;
                                playlist.add(mVideo);
                                if (mCallback != null) {
                                    mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_ADD_TO_QUEUE);
                                }
                            }

                            closeDialog();
                            MessageHelpers.showMessage(getContext(), String.format("%s: %s",
                                    mVideo.getAuthor(),
                                    getContext().getString(containsVideo ? R.string.removed_from_playback_queue : R.string.added_to_playback_queue))
                            );
                        }));
    }

    private void appendShowPlaybackQueueButton() {
        if (!mIsShowPlaybackQueueButtonEnabled) {
            return;
        }

        if (mVideo == null || !mVideo.hasVideo()) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(
                        getContext().getString(R.string.action_playback_queue),
                        optionItem -> AppDialogUtil.showPlaybackQueueDialog(getContext(), video -> PlaybackPresenter.instance(getContext()).openVideo(video))
                )
        );
    }

    private void appendPlaylistOrderButton() {
        if (!mIsPlaylistOrderButtonEnabled) {
            return;
        }

        if (mVideo == null || !BrowsePresenter.instance(getContext()).isPlaylistsSection()) {
            return;
        }

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(
                        R.string.playlist_order),
                        optionItem -> AppDialogUtil.showPlaylistOrderDialog(getContext(), mVideo, mDialogPresenter::closeDialog)
                ));
    }

    private void appendStreamReminderButton() {
        if (!mIsStreamReminderButtonEnabled) {
            return;
        }

        if (mVideo == null || !mVideo.isUpcoming) {
            return;
        }

        StreamReminderService reminderService = StreamReminderService.instance(getContext());
        boolean reminderSet = reminderService.isReminderSet(mVideo);

        mDialogPresenter.appendSingleButton(
                UiOptionItem.from(getContext().getString(reminderSet ? R.string.unset_stream_reminder : R.string.set_stream_reminder),
                        optionItem -> {
                            reminderService.toggleReminder(mVideo);
                            closeDialog();
                            MessageHelpers.showMessage(getContext(), reminderSet ? R.string.msg_done : R.string.playback_starts_shortly);
                        }
                ));
    }

    private void addRemoveFromPlaylist(String playlistId, String playlistTitle, boolean add) {
        if (add) {
            Observable<Void> editObserve = mItemManager.addToPlaylistObserve(playlistId, mVideo.videoId);
            mAddToPlaylistAction = RxUtils.execute(editObserve);
            mDialogPresenter.closeDialog();
            MessageHelpers.showMessage(getContext(),
                    getContext().getString(R.string.added_to, playlistTitle));
        } else {
            // Check that the current video belongs to the right section
            if (mCallback != null && Helpers.equals(mVideo.playlistId, playlistId)) {
                mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_REMOVE_FROM_PLAYLIST);
            }
            Observable<Void> editObserve = mItemManager.removeFromPlaylistObserve(playlistId, mVideo.videoId);
            mAddToPlaylistAction = RxUtils.execute(editObserve);
            mDialogPresenter.closeDialog();
            MessageHelpers.showMessage(getContext(),
                    getContext().getString(R.string.removed_from, playlistTitle));
        }
    }

    //private void addRemoveFromPlaylist(String playlistId, String playlistTitle, boolean add) {
    //    if (add) {
    //        Observable<Void> editObserve = mItemManager.addToPlaylistObserve(playlistId, mVideo.videoId);
    //        mAddToPlaylistAction = RxUtils.execute(editObserve);
    //        mDialogPresenter.closeDialog();
    //        MessageHelpers.showMessage(getContext(),
    //                getContext().getString(R.string.added_to, playlistTitle));
    //    } else {
    //        AppDialogUtil.showConfirmationDialog(getContext(), () -> {
    //            // Check that the current video belongs to the right section
    //            if (mCallback != null && Helpers.equals(mVideo.playlistId, playlistId)) {
    //                mCallback.onItemAction(mVideo, VideoMenuCallback.ACTION_REMOVE_FROM_PLAYLIST);
    //            }
    //            Observable<Void> editObserve = mItemManager.removeFromPlaylistObserve(playlistId, mVideo.videoId);
    //            mAddToPlaylistAction = RxUtils.execute(editObserve);
    //            mDialogPresenter.closeDialog();
    //            MessageHelpers.showMessage(getContext(),
    //                    getContext().getString(R.string.removed_from, playlistTitle));
    //        }, getContext().getString(R.string.dialog_remove_from, playlistTitle));
    //    }
    //}

    private void toggleSubscribe() {
        if (mVideo == null) {
            return;
        }

        // Until synced we won't really know weather we subscribed to a channel.
        // Exclusion: channel item (can't be synced)
        // Note, regular items (from subscribed section etc) aren't contain channel id
        if (mVideo.isSynced || mVideo.canSubscribe()) {
            toggleSubscribe(mVideo);
        } else {
            MessageHelpers.showMessage(getContext(), R.string.wait_data_loading);

            mServiceManager.loadMetadata(mVideo, metadata -> {
                Video video = Video.from(mVideo);
                video.sync(metadata);
                toggleSubscribe(video);
            });
        }
    }

    private void toggleSubscribe(Video video) {
        if (video == null) {
            return;
        }

        Observable<Void> observable = video.isSubscribed ?
                mItemManager.unsubscribeObserve(video.channelId) : mItemManager.subscribeObserve(video.channelId);

        mSubscribeAction = RxUtils.execute(observable);

        video.isSubscribed = !video.isSubscribed;

        if (!video.isSubscribed && mCallback != null) {
            mCallback.onItemAction(video, VideoMenuCallback.ACTION_UNSUBSCRIBE);
        }

        MessageHelpers.showMessage(getContext(),
                video.getAuthor() + ": " + getContext().getString(!video.isSubscribed ? R.string.unsubscribed_from_channel : R.string.subscribed_to_channel));
    }

    @Override
    protected void updateEnabledMenuItems() {
        super.updateEnabledMenuItems();

        mIsAddToPlaylistButtonEnabled = true;
        mIsAddToRecentPlaylistButtonEnabled = true;
        mIsAddToPlaybackQueueButtonEnabled = true;
        mIsShowPlaybackQueueButtonEnabled = true;
        mIsOpenChannelButtonEnabled = true;
        mIsOpenChannelUploadsButtonEnabled = true;
        mIsOpenPlaylistButtonEnabled = true;
        mIsSubscribeButtonEnabled = true;
        mIsNotInterestedButtonEnabled = true;
        mIsRemoveFromHistoryButtonEnabled = true;
        mIsShareLinkButtonEnabled = true;
        mIsShareEmbedLinkButtonEnabled = true;
        mIsReturnToBackgroundVideoEnabled = true;
        mIsOpenDescriptionButtonEnabled = true;
        mIsPlayVideoButtonEnabled = true;
        mIsPlaylistOrderButtonEnabled = true;
        mIsStreamReminderButtonEnabled = true;

        MainUIData mainUIData = MainUIData.instance(getContext());

        mIsAddToRecentPlaylistButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_RECENT_PLAYLIST);
        mIsAddToPlaybackQueueButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_ADD_TO_QUEUE);
        mIsAddToPlaylistButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_ADD_TO_PLAYLIST);
        mIsShareLinkButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_SHARE_LINK);
        mIsShareEmbedLinkButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_SHARE_EMBED_LINK);
        mIsNotInterestedButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_NOT_INTERESTED);
        mIsRemoveFromHistoryButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_REMOVE_FROM_HISTORY);
        mIsOpenDescriptionButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_OPEN_DESCRIPTION);
        mIsPlayVideoButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_PLAY_VIDEO);
        mIsSubscribeButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_SUBSCRIBE);
        mIsStreamReminderButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_STREAM_REMINDER);
        mIsShowPlaybackQueueButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_SHOW_QUEUE);
        mIsPlaylistOrderButtonEnabled = mainUIData.isMenuItemEnabled(MainUIData.MENU_ITEM_PLAYLIST_ORDER);
    }
}
