// Gabs Remote: codificador Opus do microfone do tablet (decisão 0007).
// 48 kHz, mono, quadros de 20 ms (960 amostras), VOIP, FEC dentro da banda.
#include <jni.h>
#include <stdint.h>
#include <opus.h>

JNIEXPORT jlong JNICALL
Java_com_limelight_gabs_mic_OpusEncoderJni_create(JNIEnv *env, jclass clazz, jint bitrate) {
    int err = 0;
    OpusEncoder *enc = opus_encoder_create(48000, 1, OPUS_APPLICATION_VOIP, &err);
    if (err != OPUS_OK || enc == NULL) {
        return 0;
    }
    opus_encoder_ctl(enc, OPUS_SET_BITRATE(bitrate));
    opus_encoder_ctl(enc, OPUS_SET_INBAND_FEC(1));
    opus_encoder_ctl(enc, OPUS_SET_PACKET_LOSS_PERC(10));
    return (jlong) (intptr_t) enc;
}

JNIEXPORT jint JNICALL
Java_com_limelight_gabs_mic_OpusEncoderJni_encode(JNIEnv *env, jclass clazz, jlong handle, jshortArray pcm, jbyteArray out) {
    OpusEncoder *enc = (OpusEncoder *) (intptr_t) handle;
    if (enc == NULL || (*env)->GetArrayLength(env, pcm) < 960) {
        return -1;
    }
    jshort *in = (*env)->GetShortArrayElements(env, pcm, NULL);
    jint outLen = (*env)->GetArrayLength(env, out);
    jbyte *dst = (*env)->GetByteArrayElements(env, out, NULL);
    int n = opus_encode(enc, (const opus_int16 *) in, 960, (unsigned char *) dst, outLen);
    (*env)->ReleaseByteArrayElements(env, out, dst, 0);
    (*env)->ReleaseShortArrayElements(env, pcm, in, JNI_ABORT);
    return n;
}

JNIEXPORT void JNICALL
Java_com_limelight_gabs_mic_OpusEncoderJni_destroy(JNIEnv *env, jclass clazz, jlong handle) {
    OpusEncoder *enc = (OpusEncoder *) (intptr_t) handle;
    if (enc != NULL) {
        opus_encoder_destroy(enc);
    }
}
