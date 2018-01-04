package com.zyl.mp3cutter.home.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.jaeger.library.StatusBarUtil;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;
import com.zyl.mp3cutter.R;
import com.zyl.mp3cutter.common.app.MyApplication;
import com.zyl.mp3cutter.common.app.di.AppComponent;
import com.zyl.mp3cutter.common.base.BaseActivity;
import com.zyl.mp3cutter.common.base.BasePresenter;
import com.zyl.mp3cutter.common.base.IBaseView;
import com.zyl.mp3cutter.common.utils.DensityUtils;
import com.zyl.mp3cutter.common.utils.FileUtils;
import com.zyl.mp3cutter.common.utils.ScreenUtils;
import com.zyl.mp3cutter.databinding.ActivityFilechooserShowBinding;
import com.zyl.mp3cutter.home.bean.FileInfo;
import com.zyl.mp3cutter.home.bean.MusicInfo;
import com.zyl.mp3cutter.home.bean.MusicInfoDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.RuntimePermissions;


/**
 * Description: mp3文件选择页
 * Created by zouyulong on 2017/10/22.
 * Person in charge :  zouyulong
 */
@RuntimePermissions
public class FileChooserActivity extends BaseActivity<IBaseView, BasePresenter<IBaseView>, ActivityFilechooserShowBinding> implements OnClickListener {

    private CommonAdapter mAdapter;
    private String mSdcardRootPath;
    private ArrayList<MusicInfo> mMusicList = new ArrayList<>();
    public static final String EXTRA_FILE_CHOOSER = "file_chooser";
    private MusicInfoDao mDao;
    private ObjectAnimator mMoveAnim;
    private int mUpdateBtnLeft;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.transition.Transition transition = TransitionInflater.from(this).inflateTransition(R.transition.explode);
            getWindow().setEnterTransition(transition);
        }
        StatusBarUtil.setColor(this, Color.TRANSPARENT);
        mDataBinding.btnUpdate.setOnClickListener(this);
        mDataBinding.btnUpdate.measure(0, 0);
        mUpdateBtnLeft = ScreenUtils.getScreenSize(this)[0] -
                mDataBinding.btnUpdate.getMeasuredWidth() - DensityUtils.dp2px(this ,10);
        initToolbar();
        mSdcardRootPath = Environment.getExternalStorageDirectory()
                .getAbsolutePath();
        mDataBinding.rlMusice.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CommonAdapter<MusicInfo>(this, R.layout.item_musicfile, mMusicList) {
            @Override
            protected void convert(ViewHolder holder, final MusicInfo musicInfo, int position) {
                holder.setText(R.id.tv_name, musicInfo.getFilename());
                holder.setText(R.id.tv_size, musicInfo.getFileSize());
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickItem(musicInfo);
                    }
                });
            }
        };
        mDataBinding.rlMusice.setAdapter(mAdapter);
        mDataBinding.rlMusice.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        mDao = MyApplication.getInstances().
                getDaoSession().getMusicInfoDao();
        FileChooserActivityPermissionsDispatcher.refreshDataWithPermissionCheck(this, false);
    }

    @Override
    protected void ComponentInject(AppComponent appComponent) {

    }

    @Override
    protected int initLayoutResId() {
        return R.layout.activity_filechooser_show;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    private void clickItem(MusicInfo info) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_FILE_CHOOSER, info.getFilepath());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void refreshData(final boolean isforce) {
        if (mDataBinding.aviLoading.isShown())
            return;
        startLoadingAnim();
        loadFile(isforce);
    }

    private void loadFile(final boolean isforce){
        Observable.create(new ObservableOnSubscribe() {
            @Override
            public void subscribe(ObservableEmitter e) throws Exception {
                List<MusicInfo> datas = mDao.loadAll();
                if (datas.size() > 0 && !isforce) {
                } else {
                    datas = new ArrayList<>();
                    updateFileItems(datas, mSdcardRootPath);
                    mDao.deleteAll();
                    if(datas!=null) {
                        mDao.insertInTx(datas);
                    }
                }
                e.onNext(datas);
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<List<MusicInfo>>() {
                    @Override
                    public void accept(List<MusicInfo> datas) throws Exception {
                        mMusicList.clear();
                        mMusicList.addAll(datas);
                        stopLoadingAnim();
                        if (mAdapter != null) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void startLoadingAnim() {
        mDataBinding.aviLoading.setVisibility(View.VISIBLE);
        mMoveAnim  = ObjectAnimator.ofFloat(mDataBinding.aviLoading, "translationX", 0f, mUpdateBtnLeft);
        mMoveAnim.setDuration(2000);
        mMoveAnim.setRepeatCount(ValueAnimator.INFINITE);
        mMoveAnim.start();
    }

    private void stopLoadingAnim(){
        mMoveAnim.cancel();
        mDataBinding.aviLoading.setVisibility(View.GONE);
    }

    private void updateFileItems(List<MusicInfo> list, String filePath) {
        File[] files = folderScan(filePath);
        if (files == null)
            return;
        File file;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isHidden())
                continue;
            String fileAbsolutePath = files[i].getAbsolutePath();
            String fileName = files[i].getName();
            boolean isDirectory = false;
            if (files[i].isDirectory()) {
                isDirectory = true;
            }
            FileInfo fileInfo = new FileInfo(fileAbsolutePath, fileName,
                    isDirectory);
            if (fileInfo.isDirectory())
                updateFileItems(list, fileInfo.getFilePath());
            else if (fileInfo.isMUSICFile()) {
                String path = fileInfo.getFilePath();
                file = new File(path);
                String size = FileUtils.getFormatFileSizeForFile(file);
                MusicInfo music = new MusicInfo(null, fileInfo.getFilePath(),
                        fileInfo.getFileName(), size);
                list.add(music);
            }
        }
    }

    private File[] folderScan(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        return files;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            backProcess();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void backProcess() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_update:
                FileChooserActivityPermissionsDispatcher.refreshDataWithPermissionCheck(this, true);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        FileChooserActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onRecordNeverAskAgain() {
        Toast.makeText(FileChooserActivity.this, getResources().getString(R.string.filechoose_permission_denied),
                Toast.LENGTH_SHORT).show();
    }
}