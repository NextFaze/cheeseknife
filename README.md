# cheeseknife

![Logo](website/static/cheeseknife_logo.png)

Field and method binding for Android views using reflection. Inspired by JakeWharton/butterknife

## Example usage

```java
class ExampleActivity extends Activity {
  @Bind(id = R.id.user) EditText usernameView;  // R.id.username_view by default
  @Bind EditText passwordView;  // R.id.password_view by default

  @OnClick(id = R.id.submit) void submit() {
    // TODO call server...
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.simple_activity);
    CheeseKnife.bind(this);
  }
}
```

## Download

Download via Maven:
```xml
<dependency>
  <groupId>com.nextfaze</groupId>
  <artifactId>cheeseknife</artifactId>
  <version>1.0.0</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.nextfaze:cheeseknife:1.0.0'
```
