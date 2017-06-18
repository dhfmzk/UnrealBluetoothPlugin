// Fill out your copyright notice in the Description page of Project Settings.

#pragma once

#include "CoreMinimal.h"

#if PLATFORM_ANDROID
#include "../../../Core/Public/Android/AndroidApplication.h"
#include "../../../Launch/Public/Android/AndroidJNI.h"
#include <android/log.h>
#endif

#include "BluetoothPlugin.generated.h"


UCLASS(Blueprintable, BlueprintType)
class UBluetoothPlugin : public UObject {

    GENERATED_BODY()

private:
#if PLATFORM_ANDROID
#endif

public:
#if PLATFORM_ANDROID
    int SetupJNIBluetooth(JNIEnv* env);
    JNIEnv* ENV = NULL;
    static jmethodID jToast;
    static jmethodID AndroidThunkJava_StartBluetooth;
    static jmethodID AndroidThunkJava_StopBluetooth;
    static jmethodID AndroidThunkJava_SearchBluetooth;
    static jmethodID AndroidThunkJava_ConnectBluetooth;
    static int length;

    void AndroidThunkCpp_StartBluetooth();
    void AndroidThunkCpp_StopBluetooth();
    void AndroidThunkCpp_SearchBluetooth();
    void AndroidThunkCpp_ConnectBluetooth(FString& _device);
    static char* rawDataAndroid;
#endif

    UBluetoothPlugin(const FObjectInitializer& ObjectInitializer);
    
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category="Android Bluetooth Plugin")
    TArray<FString> mMacAddress;
    
    UFUNCTION(BlueprintPure, Category="Android Bluetooth Plugin")
    static UBluetoothPlugin* GetBluetoothInstance(bool& isValid);
    
    static char* GetLawData(bool& isValid, int& length);
};