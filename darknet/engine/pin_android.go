//go:build android

package engine

/*
#cgo LDFLAGS: -landroid
#include <android/multinetwork.h>
*/
import "C"

import "fmt"

func pinToNetwork(handle uint64, fd uintptr) error {
	if rc, err := C.android_setsocknetwork(C.net_handle_t(handle), C.int(fd)); rc != 0 {
		return fmt.Errorf("android_setsocknetwork: %w", err)
	}
	return nil
}
