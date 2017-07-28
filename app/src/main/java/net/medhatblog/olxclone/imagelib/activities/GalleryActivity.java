package net.medhatblog.olxclone.imagelib.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.medhatblog.olxclone.R;
import net.medhatblog.olxclone.imagelib.adapters.GalleryImagesAdapter;
import net.medhatblog.olxclone.imagelib.utils.Constants;
import net.medhatblog.olxclone.imagelib.utils.Image;
import net.medhatblog.olxclone.imagelib.utils.Params;
import net.medhatblog.olxclone.imagelib.utils.Utils;


import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Created by vansikrishna on 08/06/2016.
 */
public class GalleryActivity extends BaseActivity {

    RelativeLayout parentLayout;
    Toolbar toolbar;
    TextView toolbar_title;
    RecyclerView recycler_view;
    AlertDialog alertDialog;
    GalleryImagesAdapter imageAdapter;
    ArrayList<Image> imagesList = new ArrayList<>();
    ArrayList<Image> imagesListr = new ArrayList<>();
    private Params params;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        parentLayout = (RelativeLayout) findViewById(R.id.parentLayout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        recycler_view = (RecyclerView) findViewById(R.id.recycler_view);
        init();
        checkForPermissions();

        Intent intent = getIntent();
        imagesListr = intent.getParcelableArrayListExtra(Constants.KEY_BUNDLE_LIST);


    }

    private void init(){
        Utils.initToolBar(this, toolbar, true);
        toolbar_title.setText(R.string.select_images);
        if(this.getIntent() != null){
            if(this.getIntent().hasExtra(Constants.KEY_PARAMS)) {
                Object object = this.getIntent().getSerializableExtra(Constants.KEY_PARAMS);
                if(object instanceof Params)
                    params = (Params) object;
                else{
                    Utils.showLongSnack(parentLayout, "Provided serializable data is not an instance of Params object.");
                    setEmptyResult();
                }
            }
        }
        handleInputParams();
        recycler_view.setLayoutManager(new StaggeredGridLayoutManager(getColumnCount(), GridLayoutManager.VERTICAL));
    }

    private void handleInputParams() {
        if(params.getPickerLimit() == 0){
            Utils.showLongSnack(parentLayout, "Please mention the picker limit as a parameter.");
            setEmptyResult();
        }
        Utils.setViewBackgroundColor(this, toolbar, params.getToolbarColor());
        if(params.getCaptureLimit() == 0){
            params.setCaptureLimit(params.getPickerLimit());
        }
    }

    private void checkForPermissions(){
        if(hasStoragePermission(this))
            getImagesFromStorage();
        else {
            requestStoragePermissions(this, Constants.REQUEST_STORAGE_PERMS);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Constants.REQUEST_STORAGE_PERMS:
                if (validateGrantedPermissions(grantResults)) {
                    getImagesFromStorage();
                } else {
                    Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_LONG).show();
                    setEmptyResult();
                }
                break;
            case Constants.REQUEST_CAMERA_PERMISSION:

                if (validateGrantedPermissions(grantResults)) {
                cameraIntent();
                } else
                    Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_LONG).show();
                break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(alertDialog != null){
            if(alertDialog.isShowing()){
                alertDialog.dismiss();
            }
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(imageAdapter != null){
            recycler_view.setHasFixedSize(true);
            StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recycler_view.getLayoutManager();
            manager.setSpanCount(getColumnCount());
            recycler_view.setLayoutManager(manager);
            recycler_view.requestLayout();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress(){

            setEmptyResult();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPress();
            return true;
        }
        else if(item.getItemId() == R.id.action_done){
            if(imageAdapter != null)
                prepareResult();
            else
                setEmptyResult();
            return true;
        }
        else if(item.getItemId() == R.id.action_camera){
            if(hasCameraPermission(this))

            {

                cameraIntent();}
            else {
                requestCameraPermissions(this, Constants.REQUEST_CAMERA_PERMISSION);

            }
        }
        return super.onOptionsItemSelected(item);
    }








    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        return true;
    }

    private void prepareResult(){
        ArrayList<Long> selectedIDs = imageAdapter.getSelectedIDs();
        ArrayList<Image> selectedImages = new ArrayList<>(selectedIDs.size());
        for(Image image : imagesList){
            if(selectedIDs.contains(image._id))
                selectedImages.add(image);
        }
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(Constants.KEY_BUNDLE_LIST, selectedImages);

        intent.putExtra("selectedId",selectedIDs);
        setIntentResult(intent);
    }

    private void getImagesFromStorage(){
        new ApiSimulator(this).executeOnExecutor(Executors.newSingleThreadExecutor());
    }

    private void populateView(ArrayList<Image> images){
        if(imagesList == null)
            imagesList = new ArrayList<>();
        imagesList.addAll(images);
        ArrayList<Image> dupImageSet = new ArrayList<>();
        dupImageSet.addAll(imagesList);
        StaggeredGridLayoutManager mLayoutManager = new StaggeredGridLayoutManager(getColumnCount(), GridLayoutManager.VERTICAL);
        mLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recycler_view.setLayoutManager(mLayoutManager);
        imageAdapter = new GalleryImagesAdapter(this, dupImageSet, getColumnCount(), params);


        recycler_view.setAdapter(imageAdapter);
        imageAdapter.setOnHolderClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long imageId = (long) view.getTag(R.id.image_id);
                imageAdapter.setSelectedItem(view, imageId);
                setCountOnToolbar();
            }
        });
    }




    private int getColumnCount() {
        if(params.getColumnCount() != 0)
            return params.getColumnCount();
        else if(params.getThumbnailWidthInDp() != 0) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density;
            return (int) (screenWidthInDp / params.getThumbnailWidthInDp());
        }
        else {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            float screenWidthInDp = displayMetrics.widthPixels / displayMetrics.density;
            float thumbnailDpWidth = getResources().getDimension(R.dimen.thumbnail_width) / displayMetrics.density;
            return (int) (screenWidthInDp / thumbnailDpWidth);
        }
    }

    private void setCountOnToolbar(){
        if(imageAdapter.getSelectedIDs().size() > 0)
            toolbar_title.setText(""+imageAdapter.getSelectedIDs().size()+ " "+ getString(R.string.selected));
        else
            toolbar_title.setText(R.string.select_images);
    }

    private void setEmptyResult(){
        setResult(RESULT_CANCELED);
        finish();
    }

    private void setIntentResult(Intent intent){
        setResult(RESULT_OK, intent);
        finish();
    }

    public void showLimitAlert(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setTitle("Alert")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private class ApiSimulator extends AsyncTask<Void, Void, ArrayList<Image>> {
        Activity context;
        String error="";

        public ApiSimulator(Activity context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("Loading..");
        }

        @Override
        protected ArrayList<Image> doInBackground(@NonNull Void... voids) {
            ArrayList<Image> images = new ArrayList<>();
            Cursor imageCursor = null;
            try {
                final String[] columns = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.WIDTH};
                final String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";
                imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
                while (imageCursor.moveToNext()) {
                    long _id = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                    int height = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT));
                    int width = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH));
                    String imagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(_id));
                    Image image = new Image(_id, uri, imagePath,(height > width)? true: false);
                    images.add(image);
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = e.toString();
            } finally {
                if(imageCursor != null && !imageCursor.isClosed()) {
                    imageCursor.close();
                }
            }
            return images;
        }

        @Override
        protected void onPostExecute(ArrayList<Image> images) {
            super.onPostExecute(images);
            dismissProgressDialog();

            if (isFinishing()) {
                return;
            }
            if(error.length() == 0)
                populateView(images);





            else
                Utils.showLongSnack(parentLayout, error);

            Intent intent = getIntent();
            ArrayList<Long> handles = (ArrayList<Long>) intent.getSerializableExtra("selectedIdBack");
            imageAdapter.enableSelection(handles);
            setCountOnToolbar();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK){
            return;
        }
        switch (requestCode) {
            case Constants.CAMERA_INTENT:

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    public void cameraIntent(){

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);



        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {




            startActivityForResult(takePictureIntent,Constants.CAMERA_INTENT);

        }


    }





}