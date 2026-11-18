//go:build !android

package engine

import "errors"

// pinToNetwork has no meaning off Android; failing keeps pinned dials from leaking.
func pinToNetwork(uint64, uintptr) error {
	return errors.New("network pinning is only available on Android")
}
