    # Podfile for iosApp

    # Use the platform and version you specified in your build.gradle.kts
    platform :ios, '14.1'

    target 'iosApp' do
      use_frameworks!

      # This is the crucial line. It tells CocoaPods to include the
      # shared library that your Gradle build produces.
      pod 'library', :path => '../library'
    end
    
