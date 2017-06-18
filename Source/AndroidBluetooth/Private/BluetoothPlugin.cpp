// Fill out your copyright notice in the Description page of Project Settings.

#include "BluetoothPlugin.h"

#define LOG_TAG "BluetoothPlugin Log"

#if PLATFORM_ANDROID
jmethodID UBluetoothPlugin::jToast = NULL;
jmethodID UBluetoothPlugin::AndroidThunkJava_StartBluetooth = NULL;
jmethodID UBluetoothPlugin::AndroidThunkJava_StopBluetooth = NULL;
jmethodID UBluetoothPlugin::AndroidThunkJava_SearchBluetooth = NULL;
jmethodID UBluetoothPlugin::AndroidThunkJava_ConnectBluetooth = NULL;
int UBluetoothPlugin::length = 0;
char* UBluetoothPlugin::rawDataAndroid = NULL;
#endif

UBluetoothPlugin::UBluetoothPlugin(const FObjectInitializer& _ObjectInitializer) 
    : Super(_ObjectInitializer) {

#if PLATFORM_ANDROID
    JNIEnv* env = FAndroidApplication::GetJavaEnv();
    SetupJNIBluetooth(env);
#endif

}

UBluetoothPlugin* UBluetoothPlugin::GetBluetoothInstance(bool& _isValid) {
    _isValid = false;
    UBluetoothPlugin* DataInstance = Cast<UBluetoothPlugin>(GEngine->GameSingleton);

    if(!DataInstance) {
        return NULL;
    }
    if(!DataInstance->IsValidLowLevel()) {
        return NULL;
    }

    _isValid = true;
    return DataInstance;
}

char* UBluetoothPlugin::GetLawData(bool& _isValid, int& _length) {
    _length = 0;
    _isValid = false;

#if PLATFORM_ANDROID
    _length = length;
    _isValid = true;
    return rawDataAndroid;
#endif

    return NULL;
}

#if PLATFORM_ANDROID
int UBluetoothPlugin::SetupJNIBluetooth(JNIEnv* env) {
    if (!env) {
        return JNI_ERR;
    }
    else {
        ENV = env;
    }

    AndroidThunkJava_StartBluetooth = FJavaWrapper::FindMethod(ENV, FJavaWrapper::GameActivityClassID, "AndroidThunkJava_StartBluetooth", "()V", false);
    if (!AndroidThunkJava_StartBluetooth) {
        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[ERROR] AndroidThunkJava_StartBluetooth Method cant be found :(");
        return JNI_ERR;
    }

    AndroidThunkJava_StopBluetooth = FJavaWrapper::FindMethod(ENV, FJavaWrapper::GameActivityClassID, "AndroidThunkJava_StopBluetooth", "()V", false);
    if (!AndroidThunkJava_StopBluetooth) {
        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[ERROR] AndroidThunkJava_StopBluetooth Method cant be found :( ");
        return JNI_ERR;
    }

    AndroidThunkJava_SearchBluetooth = FJavaWrapper::FindMethod(ENV, FJavaWrapper::GameActivityClassID, "AndroidThunkJava_SearchBluetooth", "()V", false);
    if (!AndroidThunkJava_SearchBluetooth) {
        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[ERROR] AndroidThunkJava_SearchBluetooth Method cant be found :( ");
        return JNI_ERR;
    }

    AndroidThunkJava_ConnectBluetooth = FJavaWrapper::FindMethod(ENV, FJavaWrapper::GameActivityClassID, "AndroidThunkJava_ConnectBluetooth", "(Ljava/lang/String;)V", false);
    if (!AndroidThunkJava_ConnectBluetooth) {
        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[ERROR] AndroidThunkJava_ConnectBluetooth Method cant be found :( ");
        return JNI_ERR;
    }

    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "module load success!!! ^_^");

    return JNI_OK;
}

void UBluetoothPlugin::AndroidThunkCpp_StartBluetooth() {
    if (!AndroidThunkJava_StartBluetooth || !ENV) {
        return;
    }
    FJavaWrapper::CallVoidMethod(ENV, FJavaWrapper::GameActivityThis, AndroidThunkJava_StartBluetooth);
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[START] Bluetooth");
}

void UBluetoothPlugin::AndroidThunkCpp_StopBluetooth() {
    if (!AndroidThunkJava_StopBluetooth || !ENV) {
        return;
    }
    FJavaWrapper::CallVoidMethod(ENV, FJavaWrapper::GameActivityThis, AndroidThunkJava_StopBluetooth);
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[STOP] Bluetooth");
}

void UBluetoothPlugin::AndroidThunkCpp_SearchBluetooth() {
    if (!AndroidThunkJava_SearchBluetooth || !ENV) {
        return;
    }
    FJavaWrapper::CallVoidMethod(ENV, FJavaWrapper::GameActivityThis, AndroidThunkJava_SearchBluetooth);
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[SEARCH] Bluetooth");
}

void UBluetoothPlugin::AndroidThunkCpp_ConnectBluetooth(FString& _device) {
    if (!AndroidThunkJava_ConnectBluetooth || !ENV) {
        return;
    }
    jstring jDeviceName = ENV->NewStringUTF(TCHAR_TO_UTF8(*_device));
    FJavaWrapper::CallVoidMethod(ENV, FJavaWrapper::GameActivityThis, AndroidThunkJava_ConnectBluetooth, jDeviceName);
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "[CONNECT] Bluetooth");
}

extern "C" {
    bool Java_com_epicgames_ue4_GameActivity_nativeSearchDevice(JNIEnv* LocalJNIEnv, jobject LocalThiz, jstring _device) {
        const char* charsName = LocalJNIEnv->GetStringUTFChars(_device, 0);
        FString DeviceName = FString(UTF8_TO_TCHAR(charsName));
        Cast<UBluetoothPlugin>(GEngine->GameSingleton)->mMacAddress.Add(DeviceName);
        LocalJNIEnv->ReleaseStringUTFChars(_device, charsName);
        return JNI_TRUE;
    }
    
    bool Java_com_epicgames_ue4_GameActivity_nativeGetPacketData(JNIEnv* LocalJNIEnv, jobject LocalThiz, jbyteArray _data) {
        int length = LocalJNIEnv->GetArrayLength(_data);
        char* buffer = new char[length];
        LocalJNIEnv->GetByteArrayRegion(_data, 0, length, reinterpret_cast<jbyte*>(buffer));
        Cast<UBluetoothPlugin>(GEngine->GameSingleton)->rawDataAndroid = buffer;
        return JNI_TRUE;
    }
}
#endif