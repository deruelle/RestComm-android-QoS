package com.cortxt.app.MMC.Sampling.Transit;

public class DrawerItem {
	private int imageView;
	private String textView;
	private int problemImageView;
	    	
	public DrawerItem() { }
	
	public DrawerItem(String textView) {
		setImage(0);
		setText(textView);
		setProblemImage(0);
	}
	
	public DrawerItem(int imageView, String textView) {
		setImage(imageView);
		setText(textView);
		setProblemImage(0);
	}
	 
	public DrawerItem(int imageView, String textView, int problemImageView) {
		setImage(imageView);
		setText(textView);
		setProblemImage(problemImageView);
	}
	     
	public String getText() {
		return this.textView;
	}
	
	public int getImage() {
		return imageView;
	}
	     
    public int getProblemImage() {
        return problemImageView;
    }

    public void setText(String text) {
        this.textView = text;
    }
     
    public void setImage(int image) {
        this.imageView = image;
    }
     
    public void setProblemImage(int problemImage) {
        this.problemImageView = problemImage;
    }
}
