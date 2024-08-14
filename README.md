# capp_SampleEPassport

This application demonstrates usage of CredenceID MRZ/EPassport APIs as well as ICAO document reading. It covers the basics
of opening the reader, calling the ICAO reading API, and then closing the sensor.

```java
ePassportOpenCommand(new EpassportReaderStatusListener() {
	@Override
	public void onEpassportReaderOpen(Biometrics.ResultCode resultCode) {
	}
	
	@Override
	public void onEpassportReaderClosed(ResultCode resultCode,
	                                    CloseReasonCode closeReasonCode) {
	}
}};

registerEpassportCardStatusListener(new OnEpassportCardStatusListener() {
	@Override
	public void onEpassportCardStatusChange(int i, int i1) {
				
	}
}};

ePassportCloseCommand()
```

```java
openMRZ(new MRZStatusListener() {
	@Override
	public void onMRZOpen(ResultCode resultCode) {
	}
	
	@Override
	public void onMRZClose(ResultCode resultCode,
			       CloseReasonCode closeReasonCode) {
	}
});

registerMrzDocumentStatusListener(new OnMrzDocumentStatusListener() {
	@Override
	public void onMrzDocumentStatusChange(int i, int i1) {
				
	}
}};

readMRZ(new OnMrzReadListener() {
	@Override
	public void onMrzRead(ResultCode resultCode,
			      String hint,
			      byte[] rawData,
			      String data,
			      String parsedData) {
	}
});

closeMRZ()
```

```java
readICAODocument(String, String, String, new Biometrics.ICAODocumentReadListener() {
	@Override
	public void onICAODocumentRead(ResultCode resultCode, 
	                               ICAOReadIntermediateCode icaoReadIntermediateCode, 
                                 String hint,
                                 ICAODocumentData icaoDocumentData) {
  }
}};
```

# Important For Developers
In order to have your code integrated into this application is MUST follow a set
of guidelines. They may seem strict, but this is to ensure the utmost perfect 
code quality, logic, and simplicity.

1.  All comments MUST follow proper grammar, punctuation, and use of English(US)
		language.

2.  All single line comments must follow the format..
    ```java
    /* This is a comment explaining something cool. */
    ```

3.  All multiline comments MUST follow the format...
    ```java
    /* This comment might explain a variable in depth, or it might explain a few
		 * lines of code. Either ways this format should be used.
     */
     ```

4.  All lines of existing code that wish to be comments out must be commented 
		using Android's comment shortcut <Ctrl-/>. This assures proper formatting. 
		Notice how after using this shortcut the comments lines look as such, with a
		noticeable gap...
    ```java
     //        private String WIFI_SSD_CREDENCEAIR = "Credence Air";
     //        private String WIFI_SSD_SQUIRREL = "Squirrel";
     ```

5.  All code MUST be formatted by pressing <Shift-Ctrl-Alt-l>. When the popup
		comes up please select all checkboxes with the following titles:

    "Whole File"
    "Optimize Imports"
    "Rearrange Code"

    This will ensure all code is cleaned, formatted, and organized in a logical
		way.

6.  Every line of code must have a good reason for existing.

7.  All methods must be commented using Java's method commenting style 
		guideline. Method declarations should also following the
		following style.
```java
    /* This method adds to numbers together and returns your their sum.
     *
     * @param one first number to use for sum
     * @param two second number to use for sum
     * @return summation of two numbers
     */
     public int
     add(final int one,
     	 final int two){
        return (one + two);
     }
 ```

     NOTES: Notice how all code was shorthanded as much as possible. Every 
		 single line exists for a reason and there is nothing more and nothing less.
		 
		 Also note how the method was defined, with the method name on a new line.
		 If there is more then one parameter, each parameter is on a new line.

8.  All global class variables MUST have a line of comment explaining their
		purpose.

9.  Every single|few lines of code should have comments explaining what they do.

10. Methods may NOT exceed past 30 lines of code. This ONLY includes ACTUAL
		lines of code. This does NOT include the method name, method parameters, 
		opening|closing brackets, comments, empty lines, etc. If a method must exceed
		more then 30 lines, it must do so with very good reason.
